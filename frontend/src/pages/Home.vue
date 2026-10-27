<template>
  <div class="home">
    <div class="left">
      <el-card>
        <template #header>筛选岗位</template>
        <div v-if="hiddenIds.length">不感兴趣的岗位：<el-button v-for="id in hiddenIds" :key="id" link @click="unhideJob(id)">恢复 #{{ id }}</el-button></div>
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
          <el-form-item label="岗位类别"><el-select v-model="filters.categoryId" clearable placeholder="全部类别">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select></el-form-item>
          <el-form-item label="最低月薪（元）"><el-input-number v-model="filters.salaryMin" :min="0" /></el-form-item>
          <el-form-item label="最高月薪（元）"><el-input-number v-model="filters.salaryMax" :min="0" /></el-form-item>
          <el-form-item label="技能">
            <el-select
              v-model="filters.skillTagIds"
              multiple
              filterable
              remote
              clearable
              :remote-method="(q) => onSearchTagsByType('SKILL', q)"
              :loading="tagLoadingMap.SKILL"
              placeholder="搜索技能"
              style="width: 100%"
            >
              <el-option v-for="t in tagOptionsMap.SKILL" :key="t.id" :label="t.tagName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="行业">
            <el-select
              v-model="filters.industryTagIds"
              multiple
              filterable
              remote
              clearable
              :remote-method="(q) => onSearchTagsByType('INDUSTRY', q)"
              :loading="tagLoadingMap.INDUSTRY"
              placeholder="搜索行业"
              style="width: 100%"
            >
              <el-option v-for="t in tagOptionsMap.INDUSTRY" :key="t.id" :label="t.tagName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="经验">
            <el-select
              v-model="filters.experienceTagIds"
              multiple
              filterable
              remote
              clearable
              :remote-method="(q) => onSearchTagsByType('EXPERIENCE', q)"
              :loading="tagLoadingMap.EXPERIENCE"
              placeholder="搜索经验"
              style="width: 100%"
            >
              <el-option v-for="t in tagOptionsMap.EXPERIENCE" :key="t.id" :label="t.tagName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-button type="primary" style="width: 100%" @click="onSearch">{{ loading ? '重新查询' : '查询' }}</el-button>
        </el-form>
      </el-card>
    </div>

    <div class="right">
      <el-alert v-if="batchStale" title="推荐已过期或数据已变化，请刷新推荐" type="warning" :closable="false" />
      <el-button v-if="batchStale" @click="resetAndFetchRecommendJobs">刷新推荐</el-button>
      <el-alert v-if="role === 'USER' && missingProfileFields.length" :title="'完善 ' + missingProfileFields.join('、') + ' 后可看到更多符合条件的岗位'" type="info" :closable="false" show-icon />
      <template v-if="!selectedJob">
        <el-tabs v-model="activeTab" class="job-tabs" @tab-change="onTabChange">
          <el-tab-pane v-if="role === 'USER'" label="推荐岗位" name="recommend" />
          <el-tab-pane label="最新岗位" name="latest" />
        </el-tabs>

        <el-card v-for="job in displayJobs" :key="job.id" class="mb job-card" @click="openDetail(job)">
          <div class="job-title-row">
            <div class="job-title">{{ job.jobName }}</div>
            <div class="job-salary">
              <span class="salary-text">{{ job.salaryRange }}</span>
              <el-tag v-if="activeTab === 'recommend' && job.match && job.match.matchScore != null" size="small">匹配分：{{ job.match.matchScore }}</el-tag>
              <el-tag v-if="activeTab === 'recommend' && job.match" size="small" type="info">推荐排序分：{{ job.match.recommendationScore }}</el-tag>
            </div>
          </div>
          <div class="job-meta">
            <span v-if="job.companyName">公司：{{ job.companyName }}</span>
            <span>城市：{{ job.city }}</span>
            <span>类型：{{ job.jobType }}</span>
            <span v-if="job.createdAt">发布时间：{{ formatDateTime(job.createdAt) }}</span>
          </div>
          <div class="job-desc">{{ job.description }}</div>
          <div v-if="job.match && job.match.matchScore == null">完善资料可获得个性化匹配</div>
          <div v-else-if="job.match">{{ job.match.reasons.join('、') }}</div>
          <div class="job-actions" @click.stop>
            <el-button v-if="role === 'USER'" size="small" @click="toggleFavorite(job)">{{ favoriteIds.has(job.id) ? '取消收藏' : '收藏' }}</el-button>
            <el-button v-if="role === 'USER' && activeTab === 'recommend'" size="small" @click="hideJob(job)">不感兴趣</el-button>
            <el-button size="small" @click="openDetail(job)">查看详情</el-button>
            <el-button v-if="role === 'USER'" size="small" type="primary" @click="onApply(job)">投递简历</el-button>
          </div>
        </el-card>
        <div ref="loadMoreRef" class="load-more-trigger">
          <span v-if="loadingMore">加载中...</span>
          <span v-else-if="!hasMore">没有更多了</span>
        </div>
      </template>

      <template v-else>
        <el-card class="detail-card">
          <template #header>
            <div class="detail-header">
              <el-button link @click="closeDetail">
                <el-icon><ArrowLeft /></el-icon>
                返回列表
              </el-button>
              <span class="detail-title">岗位详情</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="岗位名称">{{ selectedJob.jobName }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedJob.companyName" label="公司名称">{{ selectedJob.companyName }}</el-descriptions-item>
            <el-descriptions-item label="城市">{{ selectedJob.city }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ selectedJob.jobType }}</el-descriptions-item>
            <el-descriptions-item label="薪资">{{ selectedJob.salaryRange }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedJob.createdAt" label="发布时间">{{ formatDateTime(selectedJob.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <div class="job-desc-block">
            <div class="job-desc-title">岗位描述</div>
            <el-input :model-value="selectedJob.description" type="textarea" :rows="10" readonly />
          </div>
          <div v-if="selectedJob.match && selectedJob.match.eligible === false" class="job-desc-block">{{ selectedJob.match.reasons.map(eligibilityReason).concat(selectedJob.match.missingProfileFields.map(profileFieldName)).join('、') }}</div>
          <div v-else-if="selectedJob.match" class="job-desc-block">
            <div>匹配分：{{ selectedJob.match.matchScore == null ? '资料不足' : selectedJob.match.matchScore }}；推荐排序分：{{ selectedJob.match.recommendationScore }}</div>
            <div>技能：{{ dimensionText(selectedJob.match.dimensions.skill) }}；类别：{{ dimensionText(selectedJob.match.dimensions.category) }}；城市：{{ dimensionText(selectedJob.match.dimensions.city) }}；薪资：{{ dimensionText(selectedJob.match.dimensions.salary) }}</div>
            <div>满足技能：{{ selectedJob.match.matchedSkills.join('、') || '无' }}</div>
            <div>不足技能：{{ selectedJob.match.missingSkills.map(x => `${x.name}（当前 ${x.actual}，要求 ${x.required}）`).join('、') || '无' }}</div>
            <div>待完善：{{ selectedJob.match.missingProfileFields.map(profileFieldName).join('、') || '无' }}</div>
          </div>
          <div class="detail-actions">
            <el-button v-if="role === 'USER'" type="primary" @click="onApply(selectedJob)">投递简历</el-button>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { formatShanghai } from '../utils/time'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { listJobs } from '../api/job'
import { applyJob, listMyResumes } from '../api/resume'
import { searchTags } from '../api/tag'
import request from '../api/request'

const filters = reactive({ 
  keyword: '', 
  city: '', 
  jobType: '',
  categoryId: null, salaryMin: null, salaryMax: null,
  skillTagIds: [], 
  industryTagIds: [], 
  experienceTagIds: [] 
})
const route = useRoute()
const router = useRouter()

const role = ref(localStorage.getItem('role') || 'USER')
const publicLoading = ref(false)
const recommendLoading = ref(false)
const loading = computed(() => role.value === 'USER' && activeTab.value === 'recommend' ? recommendLoading.value : publicLoading.value)
const tagLoadingMap = reactive({ SKILL: false, INDUSTRY: false, EXPERIENCE: false })
const tagOptionsMap = reactive({ SKILL: [], INDUSTRY: [], EXPERIENCE: [] })

const categories = ref([])
const missingProfileFields = ref([])
const jobs = ref([])
const recommendJobs = ref([])
const resumes = ref([])
const favoriteIds = ref(new Set())
const hiddenIds = ref([])
const activeTab = ref(role.value === 'USER' ? 'recommend' : 'latest')

const selectedJob = ref(null)

const initialPageSize = 10
const pageSize = 10
const publicLoadingMore = ref(false)
const recommendLoadingMore = ref(false)
const loadingMore = computed(() => role.value === 'USER' && activeTab.value === 'recommend' ? recommendLoadingMore.value : publicLoadingMore.value)
const publicHasMore = ref(true)
const recommendHasMore = ref(true)
const publicCursorId = ref(null)
const recommendOffset = ref(0)
const batchId = ref(null)
const batchStale = ref(false)
const loadMoreRef = ref(null)
let observer = null
let recommendGeneration = 0
let activeRecommendAbort = null
let detailGeneration = 0
const scrollRoot = ref(null)
const prefetchCount = ref(0)

const displayJobs = computed(() => {
  if (role.value === 'USER' && activeTab.value === 'recommend') return recommendJobs.value
  return jobs.value
})

const hasMore = computed(() => {
  if (role.value === 'USER' && activeTab.value === 'recommend') return recommendHasMore.value
  return publicHasMore.value
})

const allTagIds = computed(() => {
  return [
    ...filters.skillTagIds,
    ...filters.industryTagIds,
    ...filters.experienceTagIds
  ]
})

function buildPublicParams() {
  return {
    keyword: filters.keyword,
    city: filters.city,
    jobType: filters.jobType,
    categoryId: filters.categoryId, salaryMin: filters.salaryMin, salaryMax: filters.salaryMax,
    tagIds: allTagIds.value.length > 0 ? allTagIds.value.join(',') : undefined,
    cursorId: publicCursorId.value,
    pageSize: publicCursorId.value == null ? initialPageSize : pageSize
  }
}

async function resetAndFetchPublicJobs() {
  publicLoading.value = true
  jobs.value = []
  publicCursorId.value = null
  publicHasMore.value = true
  prefetchCount.value = 0
  try {
    await loadMore()
  } finally {
    publicLoading.value = false
  }
}

async function loadMorePublicJobs() {
  if (!publicHasMore.value || publicLoadingMore.value) return
  publicLoadingMore.value = true
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
    publicLoadingMore.value = false
  }
}

async function resetAndFetchRecommendJobs() {
  if (role.value !== 'USER') return
  const generation = ++recommendGeneration
  activeRecommendAbort?.abort()
  activeRecommendAbort = null
  recommendLoadingMore.value = false
  recommendLoading.value = true
  recommendJobs.value = []
  recommendOffset.value = 0
  batchId.value = null
  batchStale.value = false
  recommendHasMore.value = true
  prefetchCount.value = 0
  try {
    await loadMoreRecommendJobs(generation)
  } catch (e) {
    if (generation === recommendGeneration) recommendJobs.value = []
  } finally {
    if (generation === recommendGeneration) recommendLoading.value = false
  }
}

async function onSearch() {
  router.push({ path: '/home', query: { keyword: filters.keyword || '' } })
  closeDetail()
  if (role.value === 'USER' && activeTab.value === 'recommend') {
    await resetAndFetchRecommendJobs()
    return
  }
  await resetAndFetchPublicJobs()
}

async function onSearchTagsByType(tagType, query) {
  const q = (query || '').trim()
  tagLoadingMap[tagType] = true
  try {
    const list = await searchTags({ tagType, keyword: q, limit: 20 })
    tagOptionsMap[tagType] = Array.isArray(list) ? list : []
  } catch (e) {
    tagOptionsMap[tagType] = []
  } finally {
    tagLoadingMap[tagType] = false
  }
}

async function onTabChange(name) {
  closeDetail()
  if (name !== 'recommend') {
    ++recommendGeneration
    activeRecommendAbort?.abort()
    activeRecommendAbort = null
    recommendLoadingMore.value = false
    recommendLoading.value = false
  }
  if (name === 'recommend' && role.value === 'USER') {
    await resetAndFetchRecommendJobs()
  }
  if (name === 'latest') {
    await resetAndFetchPublicJobs()
  }
}

function formatDateTime(value) { return formatShanghai(value) }
function dimensionText(value) { return value == null ? '资料不足' : `${(value * 100).toFixed(1)} 分` }
function profileFieldName(value) { return ({ skills: '技能', expectedCategoryId: '期望岗位类别', cityCodes: '期望城市', salaryMin: '最低期望月薪', educationRank: '请填写学历', graduationYear: '请填写毕业年份' })[value] || value }
function eligibilityReason(value) { return ({ FRESH_GRADUATES_NOT_ACCEPTED: '岗位不接收应届生', EDUCATION_REQUIREMENT_NOT_MET: '学历不满足要求', GRADUATION_YEAR_REQUIREMENT_NOT_MET: '毕业年份不满足要求', ALREADY_APPLIED: '已投递该岗位', HIDDEN_BY_STUDENT: '已设为不感兴趣' })[value] || value }

async function openDetail(job) {
  const generation = ++detailGeneration
  selectedJob.value = { ...job }
  if (role.value === 'USER') await request.post(`/api/student/jobs/${job.id}/view`)
  if (role.value === 'USER' && !job.match) {
    try {
      const match = await request.get(`/api/student/matches/${job.id}`, { isCurrentRequest: () => generation === detailGeneration })
      if (generation === detailGeneration && selectedJob.value?.id === job.id) selectedJob.value.match = match
    } catch (e) {
      if (generation === detailGeneration) throw e
    }
  }
}
async function toggleFavorite(job) {
  const active=!favoriteIds.value.has(job.id)
  await request.post(`/api/student/jobs/${job.id}/actions/FAVORITE`,null,{params:{active}})
  const next=new Set(favoriteIds.value);if(active)next.add(job.id);else next.delete(job.id);favoriteIds.value=next
  if(activeTab.value==='recommend')await resetAndFetchRecommendJobs()
}
async function hideJob(job) {
  await request.post(`/api/student/jobs/${job.id}/actions/HIDE`,null,{params:{active:true}})
  hiddenIds.value=[...new Set([...hiddenIds.value,job.id])]
  await resetAndFetchRecommendJobs()
}
async function unhideJob(id) {await request.post(`/api/student/jobs/${id}/actions/HIDE`,null,{params:{active:false}});hiddenIds.value=hiddenIds.value.filter(x=>x!==id);await resetAndFetchRecommendJobs()}

function closeDetail() {
  ++detailGeneration
  selectedJob.value = null
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
  const defaultResume = resumes.value.find((r) => r.isDefault === 1 && r.status === 1)
  if (!defaultResume) {
    ElMessage.warning('请先设置有效的默认简历')
    router.push('/resume')
    return
  }
  await applyJob(job.id, defaultResume.id)
  ElMessage.success('已投递')
  if (activeTab.value === 'recommend') {
    closeDetail()
    await resetAndFetchRecommendJobs()
  }
}

async function loadMoreRecommendJobs(generation = recommendGeneration) {
  if (generation !== recommendGeneration || !recommendHasMore.value || recommendLoadingMore.value) return
  recommendLoadingMore.value = true
  const controller = new AbortController()
  activeRecommendAbort = controller
  const config = { signal: controller.signal, isCurrentRequest: () => generation === recommendGeneration }
  try {
    const size = pageSize
    const params = {
      keyword: filters.keyword || undefined,
      city: filters.city || undefined,
      jobType: filters.jobType || undefined,
      categoryId: filters.categoryId, salaryMin: filters.salaryMin, salaryMax: filters.salaryMax,
      tagIds: allTagIds.value.length > 0 ? allTagIds.value.join(',') : undefined,
      pageSize: size
    }
    let response
    if (!batchId.value) {
      delete params.pageSize
      response = await request.post('/api/student/recommendation-batches', params, config)
    } else {
      response = await request.get(`/api/student/recommendation-batches/${batchId.value}`, { ...config, params: { page: Math.floor(recommendOffset.value / size) + 1, pageSize: size } })
    }
    if (generation !== recommendGeneration) return
    if (!batchId.value) batchId.value = response.batchId
    const batch = Array.isArray(response.items) ? response.items : []
    if (batch.length === 0) {
      recommendHasMore.value = false
      return
    }
    recommendJobs.value = recommendJobs.value.concat(batch)
    recommendOffset.value = recommendOffset.value + batch.length
    if (recommendOffset.value >= response.totalRanked) {
      recommendHasMore.value = false
    }
  } catch (e) {
    if (generation !== recommendGeneration || controller.signal.aborted) return
    if (e.response?.data?.code === 'BATCH_STALE') {
      batchStale.value = true
      recommendHasMore.value = false
      recommendJobs.value = []
    } else throw e
  } finally {
    if (generation === recommendGeneration && activeRecommendAbort === controller) {
      activeRecommendAbort = null
      recommendLoadingMore.value = false
    }
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
  await Promise.all([
    role.value === 'USER' ? request.get('/api/student/job-actions').then(list=>{favoriteIds.value=new Set((list||[]).filter(x=>x.actionType==='FAVORITE'&&x.active).map(x=>x.jobId));hiddenIds.value=(list||[]).filter(x=>x.actionType==='HIDE'&&x.active).map(x=>x.jobId)}) : Promise.resolve(),
    role.value === 'USER' ? request.get('/api/student/profile').then(profile => { missingProfileFields.value = [profile.educationRank == null ? '学历' : '', profile.graduationYear == null ? '毕业年份' : ''].filter(Boolean) }) : Promise.resolve(),
    request.get('/api/public/job/categories').then(list => { categories.value = list || [] }),
    onSearchTagsByType('SKILL', ''),
    onSearchTagsByType('INDUSTRY', ''),
    onSearchTagsByType('EXPERIENCE', '')
  ])
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
    closeDetail()
    if (role.value === 'USER' && activeTab.value === 'recommend') {
      await resetAndFetchRecommendJobs()
      return
    }
    await resetAndFetchPublicJobs()
  }
)

onBeforeUnmount(() => {
  ++recommendGeneration
  ++detailGeneration
  activeRecommendAbort?.abort()
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

.job-card {
  cursor: pointer;
}

.job-card:hover {
  border-color: #409eff;
}

.job-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.job-title {
  font-weight: 600;
  min-width: 0;
}

.job-salary {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

.salary-text {
  font-weight: 500;
  font-size: small;
  color: #419eff;
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

.detail-card {
  min-height: calc(100vh - 140px);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-title {
  font-weight: 600;
  color: #303133;
}

.job-desc-block {
  margin-top: 20px;
}

.job-desc-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
