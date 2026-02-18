import request from './request'

export function sendEmailCode(email, purpose) {
  return request.post('/api/public/auth/code', { email, purpose })
}

export function loginByPassword(username, password) {
  return request.post('/api/public/auth/login/password', { username, password })
}

export function loginByEmail(email, code) {
  return request.post('/api/public/auth/login/email', { email, code })
}

export function register({ username, password, email, code, role, companyName }) {
  return request.post('/api/public/auth/register', { username, password, email, code, role, companyName })
}

export function logout() {
  return request.post('/api/common/auth/logout')
}
