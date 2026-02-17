<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>岗位管理</div>
        <el-button type="primary" size="small" @click="openCreate">发布岗位</el-button>
      </div>
    </template>

    <el-table :data="jobs" size="small" style="width: 100%">
      <el-table-column prop="jobName" label="岗位名称" min-width="160" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="jobType" label="类型" width="100" />
      <el-table-column prop="salaryRange" label="薪资" width="130" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">招聘中</el-tag>
          <el-tag v-else type="info">下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.auditStatus === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.auditStatus === 1" type="success">通过</el-tag>
          <el-tag v-else type="danger">拒绝</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="投递数" width="80">
        <template #default="{ row }">
          <span>{{ row.applyCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="查看简历" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openApplies(row)">查看简历</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="openTags(row)">标签</el-button>
          <el-popconfirm title="确认下架该岗位？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger">下架</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editVisible" :title="editForm.id ? '编辑岗位' : '发布岗位'" width="840px">
    <el-form :model="editForm" label-position="top">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="岗位名称">
            <el-input v-model="editForm.jobName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="公司名称">
            <el-input :model-value="(profile && profile.companyName) || ''" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="城市">
            <el-input v-model="editForm.city" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="岗位类型">
            <el-input v-model="editForm.jobType" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="薪资范围">
            <el-input v-model="editForm.salaryRange" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-switch
              v-model="editForm.status"
              :active-value="1"
              :inactive-value="0"
              active-text="招聘中"
              inactive-text="下架"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="岗位描述">
            <el-input v-model="editForm.description" type="textarea" :rows="10" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="tagVisible" title="配置岗位标签" width="720px">
    <div v-if="currentJob" class="tag-tip">当前岗位：{{ currentJob.jobName }}</div>
    <el-table :data="tags" size="small" style="width: 100%; margin-top: 8px">
      <el-table-column label="选择" width="60">
        <template #default="{ row }">
          <el-checkbox v-model="checkedMap[row.id]" />
        </template>
      </el-table-column>
      <el-table-column prop="tagName" label="标签" min-width="160" />
      <el-table-column prop="tagType" label="类型" width="120" />
      <el-table-column label="熟练度(技能)" width="160">
        <template #default="{ row }">
          <el-select v-if="row.tagType === 'SKILL'" v-model="proficiencyMap[row.id]" style="width: 120px">
            <el-option label="了解(1)" :value="1" />
            <el-option label="熟悉(2)" :value="2" />
            <el-option label="精通(3)" :value="3" />
          </el-select>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="tagVisible = false">取消</el-button>
      <el-button type="primary" @click="saveTags">保存标签</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="applyVisible" title="投递简历列表" width="860px">
    <div class="apply-filters">
      <el-select v-model="applyFilter.education" size="small" clearable placeholder="学历" style="width: 140px">
        <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
      </el-select>
      <el-select
        v-model="applyFilter.tagIds"
        size="small"
        multiple
        filterable
        clearable
        placeholder="标签筛选"
        style="width: 320px"
      >
        <el-option v-for="t in tags" :key="t.id" :label="t.tagName" :value="t.id" />
      </el-select>
      <el-input-number v-model="applyFilter.minScore" size="small" :min="0" :max="1" :step="0.05" controls-position="right" />
      <div class="apply-filter-tip">最低匹配度</div>
      <el-button size="small" @click="resetApplyFilter">重置</el-button>
    </div>

    <el-table :data="filteredApplications" size="small" style="width: 100%">
      <el-table-column prop="resume.realName" label="姓名" width="100" />
      <el-table-column prop="resume.education" label="学历" width="110" />
      <el-table-column prop="resume.city" label="城市" width="100" />
      <el-table-column prop="matchScore" label="匹配度" width="100" />
      <el-table-column label="标签" min-width="180">
        <template #default="{ row }">
          <div class="apply-tags-row">
            <el-tag v-for="t in getVisibleTags(row.resumeTags)" :key="t.tagId" size="small" effect="plain">
              {{ t.tagName }}
            </el-tag>
            <span v-if="(row.resumeTags || []).length > maxVisibleTags" class="more-tags">
              +{{ (row.resumeTags || []).length - maxVisibleTags }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="applyStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.applyStatus === 0 || row.applyStatus === 1" type="info">已投递</el-tag>
          <el-tag v-else-if="row.applyStatus === 2" type="success">通过</el-tag>
          <el-tag v-else type="danger">拒绝</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="updateApply(row, 2)">通过</el-button>
          <el-button size="small" type="danger" @click="updateApply(row, 3)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="applyVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="resumeVisible" title="简历详情" size="780px">
    <el-descriptions v-if="currentResume" :column="2" border>
      <el-descriptions-item label="简历名称">{{ currentResume.resumeName }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ currentResume.realName }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ currentResume.city }}</el-descriptions-item>
      <el-descriptions-item label="学历">{{ currentResume.education }}</el-descriptions-item>
      <el-descriptions-item label="工作年限">{{ currentResume.workYears }}</el-descriptions-item>
      <el-descriptions-item label="期望岗位">{{ currentResume.expectJobType }}</el-descriptions-item>
      <el-descriptions-item label="期望薪资">{{ currentResume.expectSalary }}</el-descriptions-item>
      <el-descriptions-item v-if="currentResume.fileUrl" label="附件简历">
        <a :href="toAbsoluteUrl(currentResume.fileUrl)" target="_blank" rel="noopener noreferrer">
          {{ currentResume.fileName || currentResume.fileUrl }}
        </a>
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="currentResume?.workDesc" class="work-desc-block">
      <div class="work-desc-title">工作描述</div>
      <el-input :model-value="currentResume.workDesc" type="textarea" :rows="10" readonly />
    </div>

    <div v-if="currentResumeTags.length > 0" class="resume-tag-block">
      <div class="resume-tag-title">简历标签</div>
      <div class="apply-tags">
        <el-tag v-for="t in currentResumeTags" :key="t.tagId" size="small" effect="plain">
          {{ t.tagName }}
        </el-tag>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTags } from '../api/tag'
import { getProfile } from '../api/user'
import {
  hrDeleteJob,
  hrListJobTags,
  hrListApplications,
  hrListJobs,
  hrSaveJob,
  hrSaveJobTags,
  hrUpdateApplyStatus
} from '../api/job'

const jobs = ref([])
const tags = ref([])
const profile = ref(null)

const editVisible = ref(false)
const tagVisible = ref(false)
const applyVisible = ref(false)
const resumeVisible = ref(false)

const currentJob = ref(null)
const applications = ref([])
const currentResume = ref(null)
const currentResumeTags = ref([])

const maxVisibleTags = 4

const editForm = reactive({
  id: null,
  jobName: '',
  city: '',
  salaryRange: '',
  jobType: '',
  description: '',
  status: 1
})

const checkedMap = reactive({})
const proficiencyMap = reactive({})
const applyFilter = reactive({ education: '', tagIds: [], minScore: null })

const educationOptions = computed(() => {
  const set = new Set()
  ;(applications.value || []).forEach((a) => {
    const e = a && a.resume && a.resume.education
    if (e) set.add(e)
  })
  return Array.from(set)
})

const filteredApplications = computed(() => {
  const list = Array.isArray(applications.value) ? applications.value : []
  const edu = (applyFilter.education || '').trim()
  const tagIds = Array.isArray(applyFilter.tagIds) ? applyFilter.tagIds : []
  const minScore = applyFilter.minScore
  return list.filter((a) => {
    if (edu && (!a.resume || a.resume.education !== edu)) return false
    if (minScore != null) {
      const s = a.matchScore == null ? null : Number(a.matchScore)
      if (s == null || Number.isNaN(s) || s < minScore) return false
    }
    if (tagIds.length > 0) {
      const ids = (a.resumeTags || []).map((t) => t.tagId)
      const hit = tagIds.some((id) => ids.includes(id))
      if (!hit) return false
    }
    return true
  })
})

function getVisibleTags(tags) {
  if (!tags || tags.length <= maxVisibleTags) return tags || []
  return tags.slice(0, maxVisibleTags)
}

async function loadJobs() {
  jobs.value = await hrListJobs()
}

async function loadProfile() {
  try {
    profile.value = await getProfile()
  } catch (e) {
    profile.value = null
  }
}

async function loadTags() {
  tags.value = await listTags()
  tags.value.forEach((t) => {
    if (typeof proficiencyMap[t.id] === 'undefined') proficiencyMap[t.id] = 1
    if (typeof checkedMap[t.id] === 'undefined') checkedMap[t.id] = false
  })
}

function openCreate() {
  Object.assign(editForm, {
    id: null,
    jobName: '',
    city: '',
    salaryRange: '',
    jobType: '',
    description: '',
    status: 1
  })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  editVisible.value = true
}

async function save() {
  await hrSaveJob(editForm)
  ElMessage.success('保存成功（岗位将进入待审核）')
  editVisible.value = false
  await loadJobs()
}

async function remove(row) {
  await hrDeleteJob(row.id)
  ElMessage.success('已下架')
  await loadJobs()
}

async function openTags(row) {
  currentJob.value = row
  Object.keys(checkedMap).forEach((k) => (checkedMap[k] = false))
  const list = await hrListJobTags(row.id)
  ;(list || []).forEach((t) => {
    checkedMap[t.tagId] = true
    if (t.proficiency != null) proficiencyMap[t.tagId] = t.proficiency
  })
  tagVisible.value = true
}

async function saveTags() {
  const items = tags.value
    .filter((t) => checkedMap[t.id])
    .map((t) => ({
      tagId: t.id,
      proficiency: t.tagType === 'SKILL' ? proficiencyMap[t.id] : null
    }))
  await hrSaveJobTags(currentJob.value.id, items)
  ElMessage.success('标签已保存')
  tagVisible.value = false
}

async function openApplies(row) {
  currentJob.value = row
  applications.value = await hrListApplications(row.id)
  applyVisible.value = true
  resetApplyFilter()
}

async function updateApply(row, status) {
  await hrUpdateApplyStatus(row.applyId, status)
  ElMessage.success('已更新')
  applications.value = await hrListApplications(currentJob.value.id)
}

function resetApplyFilter() {
  applyFilter.education = ''
  applyFilter.tagIds = []
  applyFilter.minScore = null
}

function openResumeDetail(row) {
  currentResume.value = (row && row.resume) || null
  currentResumeTags.value = (row && row.resumeTags) || []
  resumeVisible.value = true
}

function toAbsoluteUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + path
}

onMounted(async () => {
  await loadJobs()
  await loadTags()
  await loadProfile()
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tag-tip {
  font-size: 12px;
  color: #606266;
}

.apply-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.apply-filter-tip {
  font-size: 12px;
  color: #606266;
}

.apply-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.apply-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 28px;
  overflow: hidden;
}

.more-tags {
  font-size: 12px;
  color: #909399;
  line-height: 22px;
}

.resume-tag-block {
  margin-top: 12px;
}

.resume-tag-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.work-desc-block {
  margin-top: 16px;
}

.work-desc-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
</style>
