import request from './request'

export function listTags(tagType) {
  return request.get('/api/public/tag/list', { params: { tagType } })
}

export function searchTags(params) {
  return request.get('/api/public/tag/search', { params })
}

export function adminListTags(params) {
  return request.get('/api/admin/tag/list', { params })
}

export function adminListTagsPage(params) {
  return request.get('/api/admin/tag/page', { params })
}

export function adminSaveTag(payload) {
  return request.post('/api/admin/tag/save', payload)
}

export function adminDeleteTag(id) {
  return request.post('/api/admin/tag/delete', { id })
}
