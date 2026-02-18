import request from './request'

export function adminListUsersPage(params) {
  return request.get('/api/admin/user/page', { params })
}

export function adminUpdateUserStatus(id, status) {
  return request.post('/api/admin/user/status/update', { id, status })
}

export function adminDeleteUser(id) {
  return request.post('/api/admin/user/delete', { id })
}
