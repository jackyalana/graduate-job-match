import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  withCredentials: true
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.satoken = token
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    if (resp && resp.config && (resp.config.responseType === 'blob' || resp.config.responseType === 'arraybuffer')) {
      return resp
    }
    const data = resp.data
    if (data && typeof data.code !== 'undefined') {
      if (data.code === 0) return data.data
      if (data.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        ElMessage.error(data.message || '未登录')
        window.location.href = '/login'
        return Promise.reject(new Error(data.message || '未登录'))
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => Promise.reject(error)
)

export default request
