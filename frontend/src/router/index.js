import { createRouter, createWebHistory } from 'vue-router'
import request from '../api/request'
import Layout from '../pages/Layout.vue'
import Login from '../pages/Login.vue'
import Register from '../pages/Register.vue'
import Home from '../pages/Home.vue'
import Profile from '../pages/Profile.vue'
import StudentProfile from '../pages/StudentProfile.vue'
import ResumeManage from '../pages/ResumeManage.vue'
import ApplyManage from '../pages/ApplyManage.vue'
import JobManage from '../pages/JobManage.vue'
import TagManage from '../pages/TagManage.vue'
import NoticeManage from '../pages/NoticeManage.vue'
import AuditManage from '../pages/AuditManage.vue'
import UserManage from '../pages/UserManage.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  {
    path: '/',
    component: Layout,
    children: [
      { path: 'home', component: Home },
      { path: 'profile', component: Profile },
      { path: 'student-profile', component: StudentProfile },
      { path: 'resume', component: ResumeManage },
      { path: 'apply', component: ApplyManage },
      { path: 'job', component: JobManage },
      { path: 'audit', component: AuditManage },
      { path: 'tag', component: TagManage },
      { path: 'notice', component: NoticeManage },
      { path: 'user', component: UserManage }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let lastAuthCheckAt = 0
const AUTH_CHECK_TTL_MS = 10 * 1000

router.beforeEach(async (to) => {
  const publicPaths = ['/login', '/register']
  if (publicPaths.includes(to.path)) return true

  const token = localStorage.getItem('token')
  if (!token) return '/login'

  const now = Date.now()
  if (now - lastAuthCheckAt > AUTH_CHECK_TTL_MS) {
    try {
      await request.get('/api/common/user/me')
      lastAuthCheckAt = now
    } catch (e) {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      return '/login'
    }
  }

  const role = localStorage.getItem('role')
  if (to.path === '/student-profile' && role !== 'USER') return '/home'
  if (to.path === '/resume' && role !== 'USER') return '/home'
  if (to.path === '/apply' && role !== 'USER') return '/home'
  if (to.path === '/job' && role !== 'HR') return '/home'
  if ((to.path === '/audit' || to.path === '/tag' || to.path === '/notice' || to.path === '/user') && role !== 'ADMIN') return '/home'
  return true
})

export default router
