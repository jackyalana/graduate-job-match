import request from './request'

export function listMyResumes() {
  return request.get('/api/user/resume/list')
}

export function getResumeDetail(id) {
  return request.get('/api/user/resume/detail', { params: { id } })
}

export function saveResume(payload) {
  return request.post('/api/user/resume/save', payload)
}

export function deleteResume(id) {
  return request.post('/api/user/resume/delete', { id })
}

export function setDefaultResume(id) {
  return request.post('/api/user/resume/set-default', { id })
}

export function saveResumeTags(resumeId, items) {
  return request.post('/api/user/resume/tags/save', { resumeId, items })
}

export function uploadResumeAttachment(resumeId, file) {
  const formData = new FormData()
  formData.append('resumeId', resumeId)
  formData.append('file', file)
  return request.post('/api/user/resume/file/upload', formData)
}

export function downloadMyResumeAttachment(resumeId) {
  return request.get('/api/user/resume/file/download', { params: { resumeId }, responseType: 'blob' })
}

export function deleteMyResumeAttachment(resumeId) {
  const formData = new FormData()
  formData.append('resumeId', resumeId)
  return request.post('/api/user/resume/file/delete', formData)
}

export function applyJob(jobId, resumeId) {
  return request.post('/api/user/job/apply', { jobId, resumeId })
}

export function listMyApplies() {
  return request.get('/api/user/apply/list')
}

export function listMyApplyDetail() {
  return request.get('/api/user/apply/list/detail')
}

export function updateApplyResume(applyId, resumeId) {
  return request.post('/api/user/apply/resume/update', { applyId, resumeId })
}
