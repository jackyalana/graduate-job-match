<template>
  <el-card>
    <template #header>
      <div class="header">
        <div v-if="!showApplyList">
          <div>岗位管理</div>
          <el-button type="primary" size="small" @click="openCreate">发布岗位</el-button>
        </div>
        <div v-else class="header-back">
          <el-button link @click="closeApplyList">
            <el-icon><ArrowLeft /></el-icon>
            返回岗位管理
          </el-button>
          <span class="apply-title">投递简历列表 - {{ currentJob?.jobName }}</span>
        </div>
      </div>
    </template>

    <template v-if="!showApplyList">
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
            <el-button size="small" type="primary" @click="openApplies(row)">查看简历</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="openTags(row)">标签</el-button>
            <el-popconfirm title="确认下架该岗位？" @confirm="remove(row)">
              <template #reference>
                <el-button size="small" type="danger">下架</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确认删除该岗位？删除后不可恢复" @confirm="hardRemove(row)">
              <template #reference>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template v-else>
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
        <el-select v-model="applyFilter.matchLevel" size="small" clearable placeholder="匹配度" style="width: 140px">
          <el-option label="高匹配" value="HIGH" />
          <el-option label="中匹配" value="MEDIUM" />
          <el-option label="低匹配" value="LOW" />
        </el-select>
        <el-button size="small" @click="resetApplyFilter">重置</el-button>
      </div>

      <el-table :data="filteredApplications" size="small" style="width: 100%">
        <el-table-column prop="resume.realName" label="姓名" width="100" />
        <el-table-column prop="applicantPhone" label="手机号" width="130" />
        <el-table-column prop="resume.education" label="学历" width="110" />
        <el-table-column prop="resume.city" label="城市" width="100" />
        <el-table-column label="匹配度" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.matchScore != null" size="small" :type="matchTagFromScore(row.matchScore / 100).type">
              匹配分 {{ row.matchScore }}
            </el-tag>
            <span v-else>资料不足或不符合条件</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tooltip v-if="resumeTagText(row.resumeTags)" :content="resumeTagText(row.resumeTags)" placement="top">
              <span class="apply-tags-ellipsis">{{ resumeTagText(row.resumeTags) }}</span>
            </el-tooltip>
            <span v-else class="empty-text">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="applyStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag>{{ applyStatusText(row.applyStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="查看简历" width="100">
          <template #default="{ row }">
            <el-button v-if="row.snapshotAvailable" size="small" type="primary" @click="openResumeDetail(row)">查看简历</el-button>
            <span v-else>历史快照不可用</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button v-if="row.applyStatus === 0" size="small" @click="updateApply(row, 1)">标记查看</el-button>
            <el-button v-if="row.applyStatus === 0 || row.applyStatus === 1" size="small" @click="updateApply(row, 2)">邀请面试</el-button>
            <el-button v-if="row.applyStatus === 2" size="small" type="success" @click="updateApply(row, 4)">发出录用</el-button>
            <el-button v-if="[0,1,2].includes(row.applyStatus)" size="small" type="danger" @click="updateApply(row, 3)">拒绝</el-button>
            <el-button size="small" @click="showTimeline(row)">时间线</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
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
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="岗位类别"><el-select v-model="editForm.categoryId" clearable>
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="城市代码"><el-input v-model="editForm.cityCode" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="最低月薪（元）"><el-input-number v-model="editForm.salaryMin" :min="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最高月薪（元）"><el-input-number v-model="editForm.salaryMax" :min="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="薪资面议"><el-switch v-model="editForm.salaryNegotiable" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="最低学历"><el-select v-model="editForm.educationMinRank" clearable>
          <el-option label="专科" :value="1" /><el-option label="本科" :value="2" /><el-option label="硕士" :value="3" /><el-option label="博士" :value="4" />
        </el-select></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最早毕业年份"><el-input-number v-model="editForm.graduateYearMin" :min="1900" :max="2100" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最晚毕业年份"><el-input-number v-model="editForm.graduateYearMax" :min="1900" :max="2100" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="接受应届生"><el-switch v-model="editForm.acceptFreshGraduate" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="截止时间（北京时间）"><el-date-picker v-model="editForm.deadlineLocal" type="datetime" value-format="YYYY-MM-DD HH:mm" /></el-form-item></el-col>
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

  <el-dialog v-model="tagVisible" title="配置岗位标签" width="800px">
    <div v-if="currentJob" class="tag-tip">当前岗位：{{ currentJob.jobName }}</div>
    
    <div class="tag-search-row">
      <el-input
        v-model="tagSearchKeyword"
        size="small"
        clearable
        placeholder="搜索标签添加"
        style="width: 300px"
        @input="onTagSearch"
      />
    </div>

    <div v-if="tagSearchKeyword" class="search-results">
      <div class="section-title">搜索结果</div>
      <div v-if="searchedTags.length === 0" class="empty-tip">未找到匹配的标签</div>
      <div v-else class="tag-list">
        <div v-for="t in searchedTags" :key="t.id" class="tag-item">
          <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
          <el-tag size="small" effect="plain">{{ tagTypeLabel(t.tagType) }}</el-tag>
          <el-select v-if="t.tagType === 'SKILL' && checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
            <el-option label="了解" :value="1" />
            <el-option label="熟悉" :value="2" />
            <el-option label="精通" :value="3" />
          </el-select>
        </div>
      </div>
    </div>

    <div v-else>
      <div class="tag-section">
        <div class="section-title">已配置标签</div>
        <div v-if="selectedTags.length === 0" class="empty-tip">暂未配置标签</div>
        <div v-else class="tag-list">
          <div v-for="t in selectedTags" :key="t.id" class="tag-item">
            <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            <el-tag size="small" effect="plain">{{ tagTypeLabel(t.tagType) }}</el-tag>
            <el-select v-if="t.tagType === 'SKILL' && checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
              <el-option label="了解" :value="1" />
              <el-option label="熟悉" :value="2" />
              <el-option label="精通" :value="3" />
            </el-select>
          </div>
        </div>
      </div>

      <el-collapse v-model="tagCollapseActive">
        <el-collapse-item title="技能标签" name="SKILL">
          <div v-if="skillTags.length === 0" class="empty-tip">暂无技能标签</div>
          <div v-else class="tag-list">
            <div v-for="t in skillTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
              <el-select v-if="checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
                <el-option label="了解" :value="1" />
                <el-option label="熟悉" :value="2" />
                <el-option label="精通" :value="3" />
              </el-select>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item title="行业标签" name="INDUSTRY">
          <div v-if="industryTags.length === 0" class="empty-tip">暂无行业标签</div>
          <div v-else class="tag-list">
            <div v-for="t in industryTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item title="经验标签" name="EXPERIENCE">
          <div v-if="experienceTags.length === 0" class="empty-tip">暂无经验标签</div>
          <div v-else class="tag-list">
            <div v-for="t in experienceTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <template #footer>
      <el-button @click="tagVisible = false">取消</el-button>
      <el-button type="primary" @click="saveTags">保存标签</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="resumeVisible" title="简历详情" size="780px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="简历名称">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.resumeName) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="姓名">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.realName) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="城市">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.city) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="学历">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.education) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="工作年限">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.workYears) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="期望岗位">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.expectJobType) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="期望薪资">
        <span :class="{ 'empty-text': !currentResume }">{{ showResumeField(currentResume && currentResume.expectSalary) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="附件简历">
        <template v-if="currentResume && currentResume.fileUrl">
          <el-button link type="primary" @click="downloadCurrentResumeAttachment">
            下载：{{ currentResume.fileName || currentResume.fileUrl }}
          </el-button>
        </template>
        <template v-else>
          <span class="empty-text">未上传简历</span>
        </template>
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
import { ArrowLeft } from '@element-plus/icons-vue'
import { listTags, searchTags } from '../api/tag'
import { getProfile } from '../api/user'
import {
  hrDeleteJob,
  hrDownloadResumeAttachment,
  hrRemoveJob,
  hrListJobTags,
  hrListApplications,
  hrListJobs,
  hrSaveJob,
  hrSaveJobTags,
  hrUpdateApplyStatus
} from '../api/job'
import { downloadBlobResponse } from '../utils/download'
import request from '../api/request'
import { matchLevelFromScore, matchTagFromScore } from '../utils/match'

const jobs = ref([])
const tags = ref([])
const profile = ref(null)

const editVisible = ref(false)
const tagVisible = ref(false)
const resumeVisible = ref(false)
const showApplyList = ref(false)
const categories = ref([])

const currentJob = ref(null)
const applications = ref([])
const currentResume = ref(null)
const currentResumeTags = ref([])
const currentApplyId = ref(null)
const tagSearchKeyword = ref('')
const searchedTags = ref([])
const tagCollapseActive = ref([])

const maxVisibleTags = 4

const editForm = reactive({
  id: null,
  jobName: '',
  city: '',
  salaryRange: '',
  jobType: '',
  description: '',
  status: 1, categoryId: null, cityCode: null, salaryMin: null, salaryMax: null, salaryNegotiable: false,
  educationMinRank: null, graduateYearMin: null, graduateYearMax: null, acceptFreshGraduate: false, deadlineLocal: null, version: null
})

const checkedMap = reactive({})
const proficiencyMap = reactive({})
const applyFilter = reactive({ education: '', tagIds: [], matchLevel: '' })

const selectedTags = computed(() => {
  return tags.value.filter((t) => checkedMap[t.id])
})

const skillTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'SKILL' && !checkedMap[t.id])
})

const industryTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'INDUSTRY' && !checkedMap[t.id])
})

const experienceTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'EXPERIENCE' && !checkedMap[t.id])
})

function tagTypeLabel(type) {
  const labels = { SKILL: '技能', INDUSTRY: '行业', EXPERIENCE: '经验' }
  return labels[type] || type
}

async function onTagSearch() {
  const kw = (tagSearchKeyword.value || '').trim()
  if (!kw) {
    searchedTags.value = []
    return
  }
  const list = await searchTags({ keyword: kw, limit: 20 })
  searchedTags.value = (list || []).filter((t) => !checkedMap[t.id])
}

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
  const matchLevel = (applyFilter.matchLevel || '').trim()
  return list.filter((a) => {
    if (edu && (!a.resume || a.resume.education !== edu)) return false
    if (matchLevel) {
      if (a.matchScore == null || matchLevelFromScore(a.matchScore / 100) !== matchLevel) return false
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

function resumeTagText(tags) {
  const list = Array.isArray(tags) ? tags : []
  return list
    .map((t) => (t && t.tagName ? String(t.tagName).trim() : ''))
    .filter((s) => s)
    .join('、')
}

async function loadJobs() {
  categories.value = await request.get('/api/public/job/categories')
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
    status: 1, categoryId: null, cityCode: null, salaryMin: null, salaryMax: null, salaryNegotiable: false,
    educationMinRank: null, graduateYearMin: null, graduateYearMax: null, acceptFreshGraduate: false, deadlineLocal: null, version: null
  })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  editForm.deadlineLocal = row.deadline ? new Date(new Date(row.deadline.replace(' ', 'T')+'Z').getTime()+8*3600*1000).toISOString().slice(0,16).replace('T',' ') : null
  editVisible.value = true
}

async function save() {
  const payload = { ...editForm, deadline: editForm.deadlineLocal ? new Date(new Date(editForm.deadlineLocal.replace(' ', 'T')+'Z').getTime()-8*3600*1000).toISOString().slice(0,16).replace('T',' ') : null }
  delete payload.deadlineLocal
  await hrSaveJob(payload)
  ElMessage.success('保存成功（岗位将进入待审核）')
  editVisible.value = false
  await loadJobs()
}

async function remove(row) {
  await hrDeleteJob(row.id)
  ElMessage.success('已下架')
  await loadJobs()
}

async function hardRemove(row) {
  await hrRemoveJob(row.id)
  ElMessage.success('已删除')
  await loadJobs()
}

async function openTags(row) {
  currentJob.value = row
  tagSearchKeyword.value = ''
  searchedTags.value = []
  tagCollapseActive.value = []
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
  await loadJobs()
}

async function openApplies(row) {
  currentJob.value = row
  applications.value = await hrListApplications(row.id)
  showApplyList.value = true
  resetApplyFilter()
}

function closeApplyList() {
  showApplyList.value = false
  currentJob.value = null
  applications.value = []
}

async function updateApply(row, status) {
  await hrUpdateApplyStatus(row.applyId, status, row.version)
  ElMessage.success('已更新')
  applications.value = await hrListApplications(currentJob.value.id)
}
function applyStatusText(status) { return ['已投递','已查看','面试中','已拒绝','已录用','已接受','已婉拒','已撤回'][status] || '未知' }
async function showTimeline(row) { const events=await request.get('/api/hr/job/apply/events',{params:{applyId:row.applyId}});ElMessage.info(events.map(e=>`${applyStatusText(e.toStatus)} ${formatShanghai(e.createdAt)}`).join(' → ')) }

function resetApplyFilter() {
  applyFilter.education = ''
  applyFilter.tagIds = []
  applyFilter.matchLevel = ''
}

function openResumeDetail(row) {
  currentResume.value = (row && row.resume) || null
  currentResumeTags.value = (row && row.resumeTags) || []
  currentApplyId.value = (row && row.applyId) || null
  resumeVisible.value = true
}

function showResumeField(value) {
  const v = value == null ? '' : String(value).trim()
  return v ? v : '未上传简历'
}

async function downloadCurrentResumeAttachment() {
  if (!currentApplyId.value) return
  const resp = await hrDownloadResumeAttachment(currentApplyId.value)
  const fallback = (currentResume.value && currentResume.value.fileName) || 'resume'
  downloadBlobResponse(resp, fallback)
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

.header > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-back {
  display: flex;
  align-items: center;
  gap: 16px;
}

.apply-title {
  font-weight: 600;
  color: #303133;
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

.apply-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.apply-tags-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.apply-tags-ellipsis {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.more-tags {
  font-size: 12px;
  color: #909399;
  line-height: 22px;
}

.empty-text {
  color: #909399;
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

.tag-search-row {
  margin-bottom: 12px;
}

.search-results {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  font-size: 14px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.empty-tip {
  color: #909399;
  font-size: 13px;
}

.tag-section {
  margin-bottom: 16px;
}
</style>
