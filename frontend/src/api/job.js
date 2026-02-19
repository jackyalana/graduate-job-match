import request from './request'

export function listJobs(params) {
  return request.get('/api/public/job/list', { params })
}

export function getJobDetail(id) {
  return request.get('/api/public/job/detail', { params: { id } })
}

export function recommendJobs(resumeId) {
  if (typeof resumeId === 'object' && resumeId !== null) {
    return request.get('/api/user/job/recommend', { params: resumeId })
  }
  return request.get('/api/user/job/recommend', { params: { resumeId } })
}

export function hrListJobs() {
  return request.get('/api/hr/job/list')
}

export function hrSaveJob(payload) {
  return request.post('/api/hr/job/save', payload)
}

export function hrDeleteJob(id) {
  return request.post('/api/hr/job/delete', { id })
}

export function hrRemoveJob(id) {
  return request.post('/api/hr/job/remove', { id })
}

export function hrSaveJobTags(jobId, items) {
  return request.post('/api/hr/job/tags/save', { jobId, items })
}

export function hrListJobTags(jobId) {
  return request.get('/api/hr/job/tags', { params: { jobId } })
}

export function hrListApplications(jobId) {
  return request.get('/api/hr/job/applications', { params: { jobId } })
}

export function hrUpdateApplyStatus(applyId, status) {
  return request.post('/api/hr/job/apply/status', { applyId, status })
}

export function hrDownloadResumeAttachment(applyId) {
  return request.get('/api/hr/resume/file/download', { params: { applyId }, responseType: 'blob' })
}

export function adminListPendingHr() {
  return request.get('/api/admin/audit/hr/list')
}

export function adminDecisionHr(id, pass) {
  return request.post('/api/admin/audit/hr/decision', { id, pass })
}

export function adminListPendingJob() {
  return request.get('/api/admin/audit/job/list')
}

export function adminDecisionJob(id, pass) {
  return request.post('/api/admin/audit/job/decision', { id, pass })
}

export function adminGetJobDetail(id) {
  return request.get('/api/admin/audit/job/detail', { params: { id } })
}
