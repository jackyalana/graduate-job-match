<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>投递记录</div>
        <div class="filters">
          <el-select v-model="filterStatus" size="small" clearable placeholder="状态" style="width: 120px">
            <el-option label="已投递" :value="0" />
            <el-option label="通过" :value="2" />
            <el-option label="拒绝" :value="3" />
          </el-select>
          <el-input v-model="keyword" size="small" clearable placeholder="岗位关键词" style="width: 180px" @keyup.enter="load" />
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="filteredApplies.length === 0" class="empty">暂无投递记录</div>
    <div v-else class="apply-list">
      <el-card v-for="a in filteredApplies" :key="a.applyId" class="apply-card">
        <div class="card-top">
          <div class="job-block">
            <div class="job-name">{{ a.job?.jobName || '-' }}</div>
            <div class="job-meta">
              <span>城市：{{ a.job?.city || '-' }}</span>
              <span>类型：{{ a.job?.jobType || '-' }}</span>
              <span>薪资：{{ a.job?.salaryRange || '-' }}</span>
            </div>
            <div class="job-time">
              <span v-if="a.job?.createdAt">发布时间：{{ formatDateTime(a.job.createdAt) }}</span>
              <span v-if="a.applyTime">投递时间：{{ formatDateTime(a.applyTime) }}</span>
            </div>
          </div>
          <div class="status-block">
            <el-tag :type="statusType(a.applyStatus)">{{ statusText(a.applyStatus) }}</el-tag>
          </div>
        </div>

        <div class="progress">
          <div class="progress-item is-active">
            <div class="dot" />
            <div class="label">已投递</div>
          </div>
          <div class="line" :class="{ done: isResultDone(a.applyStatus) }" />
          <div class="progress-item" :class="{ 'is-active': isResultDone(a.applyStatus), pass: a.applyStatus === 2, reject: a.applyStatus === 3 }">
            <div class="dot" />
            <div class="label">{{ resultText(a.applyStatus) }}</div>
          </div>
        </div>

        <div v-if="a.job?.description" class="job-desc-preview">{{ a.job.description }}</div>

        <div class="card-actions">
          <el-button size="small" @click="openJob(a.job)">查看岗位</el-button>
          <el-button size="small" @click="openResume(a.resume)">查看简历</el-button>
        </div>
      </el-card>
    </div>
  </el-card>

  <el-dialog v-model="jobVisible" title="岗位详情" width="840px">
    <el-descriptions v-if="currentJob" :column="2" border>
      <el-descriptions-item label="岗位名称">{{ currentJob.jobName }}</el-descriptions-item>
      <el-descriptions-item v-if="currentJob.companyName" label="公司名称">{{ currentJob.companyName }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ currentJob.city }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ currentJob.jobType }}</el-descriptions-item>
      <el-descriptions-item label="薪资">{{ currentJob.salaryRange }}</el-descriptions-item>
      <el-descriptions-item v-if="currentJob.createdAt" label="发布时间">{{ formatDateTime(currentJob.createdAt) }}</el-descriptions-item>
    </el-descriptions>
    <div v-if="currentJob?.description" class="job-desc-block">
      <div class="job-desc-title">岗位描述</div>
      <el-input :model-value="currentJob.description" type="textarea" :rows="10" readonly />
    </div>
    <template #footer>
      <el-button @click="jobVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="resumeVisible" title="简历详情" width="840px">
    <el-descriptions v-if="currentResume" :column="2" border>
      <el-descriptions-item label="简历名称">{{ currentResume.resumeName }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ currentResume.realName }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ currentResume.city }}</el-descriptions-item>
      <el-descriptions-item label="学历">{{ currentResume.education }}</el-descriptions-item>
      <el-descriptions-item label="工作年限">{{ currentResume.workYears }}</el-descriptions-item>
      <el-descriptions-item label="期望岗位">{{ currentResume.expectJobType }}</el-descriptions-item>
      <el-descriptions-item label="期望薪资">{{ currentResume.expectSalary }}</el-descriptions-item>
      <el-descriptions-item v-if="currentResume.fileUrl" label="附件简历">
        <el-button link type="primary" @click="downloadCurrentResume">
          下载：{{ currentResume.fileName || currentResume.fileUrl }}
        </el-button>
      </el-descriptions-item>
    </el-descriptions>
    <div v-if="currentResume?.workDesc" class="work-desc-block">
      <div class="work-desc-title">工作描述</div>
      <el-input :model-value="currentResume.workDesc" type="textarea" :rows="10" readonly />
    </div>
    <template #footer>
      <el-button @click="resumeVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMyApplyDetail, listMyResumes, updateApplyResume, downloadMyResumeAttachment } from '../api/resume'
import { downloadBlobResponse } from '../utils/download'

const loading = ref(false)
const applies = ref([])
const resumes = ref([])

const filterStatus = ref()
const keyword = ref('')

const resumePickMap = reactive({})
const changingMap = reactive({})

const jobVisible = ref(false)
const resumeVisible = ref(false)
const currentJob = ref(null)
const currentResume = ref(null)

const filteredApplies = computed(() => {
  const list = Array.isArray(applies.value) ? applies.value : []
  const kw = (keyword.value || '').trim()
  return list.filter((a) => {
    if (filterStatus.value != null && a.applyStatus !== filterStatus.value) return false
    if (!kw) return true
    const name = (a.job && a.job.jobName) || ''
    return name.includes(kw)
  })
})

function statusText(status) {
  if (status === 2) return '通过'
  if (status === 3) return '拒绝'
  return '已投递'
}

function statusType(status) {
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  return 'info'
}

function isResultDone(status) {
  return status === 2 || status === 3
}

function resultText(status) {
  if (status === 2) return '通过'
  if (status === 3) return '拒绝'
  return '待处理'
}

function canChangeResume(a) {
  return a && (a.applyStatus == null || a.applyStatus < 2)
}

function canSubmitChange(a) {
  if (!canChangeResume(a)) return false
  const picked = resumePickMap[a.applyId]
  if (!picked) return false
  const current = a.resume && a.resume.id
  return picked !== current
}

async function load() {
  loading.value = true
  try {
    const [applyList, resumeList] = await Promise.all([listMyApplyDetail(), listMyResumes()])
    applies.value = Array.isArray(applyList) ? applyList : []
    resumes.value = Array.isArray(resumeList) ? resumeList : []
    applies.value.forEach((a) => {
      if (resumePickMap[a.applyId] == null) {
        resumePickMap[a.applyId] = a.resume && a.resume.id
      }
    })
  } finally {
    loading.value = false
  }
}

async function onChangeResume(a) {
  if (!canSubmitChange(a)) return
  changingMap[a.applyId] = true
  try {
    await updateApplyResume(a.applyId, resumePickMap[a.applyId])
    ElMessage.success('已更换投递简历')
    await load()
  } finally {
    changingMap[a.applyId] = false
  }
}

function openJob(job) {
  currentJob.value = job || null
  jobVisible.value = true
}

function openResume(resume) {
  currentResume.value = resume || null
  resumeVisible.value = true
}

async function downloadCurrentResume() {
  if (!currentResume.value || !currentResume.value.id) return
  const resp = await downloadMyResumeAttachment(currentResume.value.id)
  downloadBlobResponse(resp, currentResume.value.fileName || 'resume')
}

function toAbsoluteUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + path
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

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.apply-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.job-block {
  min-width: 0;
}

.job-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.job-meta,
.job-time {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #606266;
}

.job-time {
  margin-top: 6px;
  color: #909399;
}

.status-block {
  flex: 0 0 auto;
}

.progress {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
  flex: 0 0 auto;
}

.progress-item.is-active {
  color: #303133;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
}

.progress-item.is-active .dot {
  background: #409eff;
}

.progress-item.pass.is-active .dot {
  background: #67c23a;
}

.progress-item.reject.is-active .dot {
  background: #f56c6c;
}

.line {
  flex: 0 0 40%;
  height: 1px;
  background: #e4e7ed;
  min-width: 120px;
  max-width: 260px;
}

.line.done {
  background: #409eff;
}

.resume-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.resume-info {
  font-size: 13px;
  color: #303133;
}

.resume-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resume-preview {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.empty {
  color: #909399;
  text-align: center;
  padding: 18px 0;
}

.job-desc-preview {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-desc-block {
  margin-top: 16px;
}

.work-desc-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.job-desc-block {
  margin-top: 16px;
}

.job-desc-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
</style>
