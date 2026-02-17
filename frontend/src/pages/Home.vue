<template>
  <div class="home">
    <div class="left">
      <el-card>
        <template #header>筛选岗位</template>
        <el-form label-position="top">
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="岗位名称 / 技能" clearable />
          </el-form-item>
          <el-form-item label="城市">
            <el-input v-model="filters.city" placeholder="如：北京" clearable />
          </el-form-item>
          <el-form-item label="岗位类型">
            <el-input v-model="filters.jobType" placeholder="如：后端开发" clearable />
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="filters.tagIds"
              multiple
              filterable
              remote
              clearable
              :remote-method="onSearchTags"
              :loading="tagLoading"
              placeholder="搜索并选择标签"
              style="width: 100%"
            >
              <el-option-group v-for="group in groupedTagOptions" :key="group.label" :label="group.label">
                <el-option v-for="t in group.options" :key="t.id" :label="t.tagName" :value="t.id" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="onSearch">查询</el-button>
        </el-form>
      </el-card>
    </div>

    <div class="right">
      <el-tabs v-model="activeTab" class="job-tabs" @tab-change="onTabChange">
        <el-tab-pane v-if="role === 'USER'" label="推荐岗位" name="recommend" />
        <el-tab-pane label="最新岗位" name="latest" />
      </el-tabs>

      <el-card v-for="job in displayJobs" :key="job.id" class="mb">
        <div class="job-title">{{ job.jobName }}</div>
        <div class="job-meta">
          <span v-if="job.companyName">公司：{{ job.companyName }}</span>
          <span>城市：{{ job.city }}</span>
          <span>类型：{{ job.jobType }}</span>
          <span>薪资：{{ job.salaryRange }}</span>
          <span v-if="job.createdAt">发布时间：{{ formatDateTime(job.createdAt) }}</span>
          <span v-if="job.matchScore != null">匹配度：{{ job.matchScore }}</span>
        </div>
        <div class="job-desc">{{ job.description }}</div>
        <div class="job-actions">
          <el-button size="small" @click="openDetail(job)">查看详情</el-button>
          <el-button v-if="role === 'USER'" size="small" type="primary" @click="onApply(job)">投递简历</el-button>
        </div>
      </el-card>
      <div ref="loadMoreRef" class="load-more-trigger">
        <span v-if="loadingMore">加载中...</span>
        <span v-else-if="!hasMore">没有更多了</span>
      </div>
    </div>
  </div>

  <el-dialog v-model="detailVisible" title="岗位详情" width="780px">
    <el-descriptions v-if="currentJob" :column="1" border>
      <el-descriptions-item label="岗位名称">{{ currentJob.jobName }}</el-descriptions-item>
      <el-descriptions-item v-if="currentJob.companyName" label="公司名称">{{ currentJob.companyName }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ currentJob.city }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ currentJob.jobType }}</el-descriptions-item>
      <el-descriptions-item label="薪资">{{ currentJob.salaryRange }}</el-descriptions-item>
      <el-descriptions-item v-if="currentJob.createdAt" label="发布时间">{{ formatDateTime(currentJob.createdAt) }}</el-descriptions-item>
      <el-descriptions-item v-if="currentJob.matchScore != null" label="匹配度">{{ currentJob.matchScore }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ currentJob.description }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button v-if="role === 'USER'" type="primary" @click="onApply(currentJob)">投递</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listJobs, recommendJobs as fetchRecommendJobsApi } from '../api/job'
import { applyJob, listMyResumes } from '../api/resume'
import { searchTags } from '../api/tag'

const filters = reactive({ keyword: '', city: '', jobType: '', tagIds: [] })
const route = useRoute()
const router = useRouter()

const role = ref(localStorage.getItem('role') || 'USER')
const loading = ref(false)
const tagLoading = ref(false)
const tagOptions = ref([])

const jobs = ref([])
const recommendJobs = ref([])
const resumes = ref([])
const activeTab = ref(role.value === 'USER' ? 'recommend' : 'latest')

const detailVisible = ref(false)
const currentJob = ref(null)

const initialPageSize = 10
const pageSize = 10
const loadingMore = ref(false)
const publicHasMore = ref(true)
const recommendHasMore = ref(true)
const publicCursorId = ref(null)
const recommendOffset = ref(0)
const loadMoreRef = ref(null)
let observer = null
const scrollRoot = ref(null)
const prefetchCount = ref(0)

const displayJobs = computed(() => {
  if (role.value === 'USER' && activeTab.value === 'recommend') return recommendJobs.value
  return jobs.value
})

const groupedTagOptions = computed(() => {
  const groups = { SKILL: [], INDUSTRY: [], EXPERIENCE: [] }
  const typeLabels = { SKILL: '技能', INDUSTRY: '行业', EXPERIENCE: '经验' }
  ;(tagOptions.value || []).forEach((t) => {
    if (groups[t.tagType]) {
      groups[t.tagType].push(t)
    }
  })
  return Object.entries(groups)
    .filter(([, opts]) => opts.length > 0)
    .map(([type, opts]) => ({ label: typeLabels[type] || type, options: opts }))
})

const hasMore = computed(() => {
  if (role.value === 'USER' && activeTab.value === 'recommend') return recommendHasMore.value
  return publicHasMore.value
})

function buildPublicParams() {
  return {
    keyword: filters.keyword,
    city: filters.city,
    jobType: filters.jobType,
    tagIds: Array.isArray(filters.tagIds) && filters.tagIds.length > 0 ? filters.tagIds.join(',') : undefined,
    cursorId: publicCursorId.value,
    pageSize: publicCursorId.value == null ? initialPageSize : pageSize
  }
}

async function resetAndFetchPublicJobs() {
  loading.value = true
  jobs.value = []
  publicCursorId.value = null
  publicHasMore.value = true
  prefetchCount.value = 0
  try {
    await loadMore()
  } finally {
    loading.value = false
  }
}

async function loadMorePublicJobs() {
  if (!publicHasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const list = await listJobs(buildPublicParams())
    const batch = Array.isArray(list) ? list : []
    if (batch.length === 0) {
      publicHasMore.value = false
      return
    }
    jobs.value = jobs.value.concat(batch)
    publicCursorId.value = batch[batch.length - 1].id
    if (batch.length < pageSize) {
      publicHasMore.value = false
    }
  } finally {
    loadingMore.value = false
  }
}

async function resetAndFetchRecommendJobs() {
  if (role.value !== 'USER') return
  loading.value = true
  recommendJobs.value = []
  recommendOffset.value = 0
  recommendHasMore.value = true
  prefetchCount.value = 0
  try {
    await loadMore()
  } catch (e) {
    recommendJobs.value = []
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  router.push({ path: '/home', query: { keyword: filters.keyword || '' } })
  if (role.value === 'USER' && activeTab.value === 'recommend') {
    await resetAndFetchRecommendJobs()
    return
  }
  await resetAndFetchPublicJobs()
}

async function onSearchTags(query) {
  const q = (query || '').trim()
  tagLoading.value = true
  try {
    tagOptions.value = await searchTags({ keyword: q, limit: 20 })
  } catch (e) {
    tagOptions.value = []
  } finally {
    tagLoading.value = false
  }
}

async function onTabChange(name) {
  if (name === 'recommend' && role.value === 'USER') {
    await resetAndFetchRecommendJobs()
  }
  if (name === 'latest') {
    await resetAndFetchPublicJobs()
  }
}

function formatDateTime(value) {
  if (!value) return ''
  if (typeof value === 'string') {
    const normalized = value.replace('T', ' ')
    const withoutMs = normalized.split('.')[0]
    return withoutMs.length >= 16 ? withoutMs.slice(0, 16) : withoutMs
  }
  if (Array.isArray(value) && value.length >= 3) {
    const year = value[0]
    const month = value[1] || 1
    const day = value[2] || 1
    const hour = value[3] || 0
    const minute = value[4] || 0
    const pad2 = (n) => String(n).padStart(2, '0')
    return `${year}-${pad2(month)}-${pad2(day)} ${pad2(hour)}:${pad2(minute)}`
  }
  try {
    const d = new Date(value)
    const pad2 = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
  } catch (e) {
    return ''
  }
}

function openDetail(job) {
  currentJob.value = job
  detailVisible.value = true
}

async function ensureResumes() {
  if (resumes.value.length > 0) return
  resumes.value = await listMyResumes()
}

async function onApply(job) {
  if (!job) return
  await ensureResumes()
  if (resumes.value.length === 0) {
    ElMessage.warning('请先创建简历')
    router.push('/resume')
    return
  }
  const defaultResume = resumes.value.find((r) => r.isDefault === 1) || resumes.value[0]
  await applyJob(job.id, defaultResume.id)
  ElMessage.success('已投递')
}

async function loadMoreRecommendJobs() {
  if (!recommendHasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const size = recommendOffset.value === 0 ? initialPageSize : pageSize
    const params = {
      keyword: filters.keyword || undefined,
      city: filters.city || undefined,
      jobType: filters.jobType || undefined,
      tagIds: Array.isArray(filters.tagIds) && filters.tagIds.length > 0 ? filters.tagIds.join(',') : undefined,
      offset: recommendOffset.value,
      pageSize: size
    }
    const list = await fetchRecommendJobsApi(params)
    const batch = Array.isArray(list) ? list : []
    if (batch.length === 0) {
      recommendHasMore.value = false
      return
    }
    recommendJobs.value = recommendJobs.value.concat(batch)
    recommendOffset.value = recommendOffset.value + batch.length
    if (batch.length < size) {
      recommendHasMore.value = false
    }
  } finally {
    loadingMore.value = false
  }
}

async function loadMore() {
  if (role.value === 'USER' && activeTab.value === 'recommend') {
    await loadMoreRecommendJobs()
    return
  }
  await loadMorePublicJobs()
}

onMounted(async () => {
  filters.keyword = route.query.keyword || ''
  await onSearchTags('')
  if (role.value === 'USER' && activeTab.value === 'recommend') {
    await resetAndFetchRecommendJobs()
  } else {
    await resetAndFetchPublicJobs()
  }

  scrollRoot.value = document.querySelector('.main')
  observer = new IntersectionObserver(
    (entries) => {
      const hit = entries.some((e) => e.isIntersecting)
      if (!hit) return
      const top = scrollRoot.value ? scrollRoot.value.scrollTop : 0
      if (!top) {
        const rootEl = scrollRoot.value
        const shouldFill = !!rootEl && rootEl.scrollHeight <= rootEl.clientHeight + 1
        if (!shouldFill && prefetchCount.value >= 1) return
        if (prefetchCount.value >= 3) return
        prefetchCount.value = prefetchCount.value + 1
      }
      loadMore()
    },
    { root: scrollRoot.value || null, rootMargin: '200px' }
  )
  if (loadMoreRef.value) {
    observer.observe(loadMoreRef.value)
  }
})

watch(
  () => route.query.keyword,
  async (val) => {
    filters.keyword = val || ''
    if (role.value === 'USER' && activeTab.value === 'recommend') {
      await resetAndFetchRecommendJobs()
      return
    }
    await resetAndFetchPublicJobs()
  }
)

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped>
.home {
  display: flex;
  gap: 16px;
  padding-top: 0px;
}

.left {
  width: 320px;
  position: sticky;
  top: 12px;
  align-self: flex-start;
}

.right {
  flex: 1;
  min-width: 0;
}

.mb {
  margin-bottom: 12px;
}

.job-tabs {
  margin-bottom: 12px;
}

.job-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.job-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.job-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.job-desc {
  font-size: 13px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.job-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.empty {
  padding: 10px 0;
  color: #909399;
  font-size: 12px;
}

.load-more-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  color: #909399;
  font-size: 12px;
}
</style>
