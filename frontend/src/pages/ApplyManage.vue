<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>投递记录</div>
        <div class="filters">
          <el-select v-model="filterStatus" size="small" clearable placeholder="状态" style="width: 120px">
            <el-option label="已投递" :value="0" />
            <el-option label="面试" :value="2" />
            <el-option label="拒绝" :value="3" />
            <el-option label="录用" :value="4" />
            <el-option label="已接受" :value="5" />
          </el-select>
          <el-input v-model="keyword" size="small" clearable placeholder="岗位关键词" style="width: 180px" @keyup.enter="load" />
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

        <div class="progress">当前进度：{{ statusText(a.applyStatus) }}（同一岗位终态后不能重复投递）</div>

        <div v-if="a.job?.description" class="job-desc-preview">{{ a.job.description }}</div>

        <div class="card-actions">
          <el-button v-if="[0,1,2].includes(a.applyStatus)" size="small" @click="changeStatus(a,7)">撤回</el-button>
          <el-button v-if="a.applyStatus === 4" size="small" type="success" @click="changeStatus(a,5)">接受录用</el-button>
          <el-button v-if="a.applyStatus === 4" size="small" @click="changeStatus(a,6)">婉拒录用</el-button>
          <el-button size="small" @click="showTimeline(a)">时间线</el-button>
          <el-button size="small" @click="openJob(a.job)">查看岗位</el-button>
          <el-button size="small" @click="openResume(a.resume,a.applyId)">查看投递简历</el-button>
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
import { formatShanghai } from '../utils/time'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMyApplyDetail, listMyResumes, updateApplyResume, downloadMyResumeAttachment } from '../api/resume'
import { downloadBlobResponse } from '../utils/download'
import request from '../api/request'

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
const currentApplyId = ref(null)

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
  return ['已投递','已查看','面试中','已拒绝','已录用','已接受','已婉拒','已撤回'][status] || '未知'
}
async function changeStatus(a,status) { await request.post('/api/user/apply/status',{applyId:a.applyId,status,version:a.version});ElMessage.success('状态已更新');await load() }
async function showTimeline(a) {const events=await request.get('/api/user/apply/events',{params:{applyId:a.applyId}});ElMessage.info(events.map(e=>`${statusText(e.toStatus)} ${formatShanghai(e.createdAt)}`).join(' → '))}

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

function openResume(resume, applyId) {
  currentResume.value = resume || null
  currentApplyId.value = applyId
  resumeVisible.value = true
}

async function downloadCurrentResume() {
  if (!currentResume.value || !currentApplyId.value) return
  const resp = await request.get('/api/user/apply/attachment',{params:{applyId:currentApplyId.value},responseType:'blob'})
  downloadBlobResponse(resp, currentResume.value.fileName || 'resume')
}

function toAbsoluteUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + path
}

function formatDateTime(value) { return formatShanghai(value) }

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
