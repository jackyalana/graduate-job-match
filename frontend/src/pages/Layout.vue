<template>
  <el-container class="page">
    <el-header class="header">
      <div class="header-left">
        <div class="logo">智能招聘系统</div>
      </div>

      <div class="header-center">
        <el-input v-model="keyword" placeholder="搜索岗位 / 内容" clearable @keyup.enter="onSearch" />
      </div>

      <div class="header-right">
        <el-popover
          v-model:visible="noticePopoverVisible"
          placement="bottom"
          :width="360"
          trigger="click"
          @show="loadNotices"
        >
          <template #reference>
            <div class="message-entry">消息</div>
          </template>
          <div class="notice-panel">
            <div class="notice-panel-title">系统公告</div>
            <div v-if="noticeLoading" class="notice-loading">加载中...</div>
            <div v-else-if="latestNotices.length === 0" class="notice-empty">暂无公告</div>
            <div v-else class="notice-list">
              <div v-for="n in latestNotices" :key="n.id" class="notice-item" @click="openNotice(n)">
                <div class="notice-item-title">{{ n.title }}</div>
                <div class="notice-item-time">{{ formatNoticeTime(n.createdAt) }}</div>
              </div>
            </div>
          </div>
        </el-popover>
        <el-tag effect="plain">{{ roleText }}</el-tag>
        <el-dropdown trigger="click" @command="onUserCommand">
          <div class="user-entry">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-caret">▾</div>
            <el-avatar :size="28" :src="avatarUrl">
              {{ avatarFallback }}
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="body">
      <el-aside :width="asideWidth" class="aside" :class="{ 'aside-collapsed': collapsed }">
        <div class="aside-top">
          <el-button class="aside-toggle" text @click="toggleCollapse">
            <el-icon v-if="collapsed"><Expand /></el-icon>
            <el-icon v-else><Fold /></el-icon>
          </el-button>
        </div>
        <div class="aside-resizer" @mousedown="onAsideResizeStart" />
        <el-menu :default-active="activePath" :collapse="collapsed" :collapse-transition="false" @select="go">
          <el-menu-item index="/home">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'USER'" index="/resume">
            <el-icon><Document /></el-icon>
            <span>简历管理</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'USER'" index="/apply">
            <el-icon><Tickets /></el-icon>
            <span>投递记录</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'HR'" index="/job">
            <el-icon><Briefcase /></el-icon>
            <span>岗位管理</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/audit">
            <el-icon><CircleCheck /></el-icon>
            <span>审核管理</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/tag">
            <el-icon><CollectionTag /></el-icon>
            <span>标签库管理</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/notice">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item v-if="role === 'ADMIN'" index="/user">
            <el-icon><User /></el-icon>
            <span>账号管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title || '公告'" width="520px">
    <div class="notice-dialog-content">{{ currentNotice?.content || '' }}</div>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Briefcase, CircleCheck, CollectionTag, Document, Expand, Fold, House, Tickets, User } from '@element-plus/icons-vue'
import { logout } from '../api/auth'
import { listNotices } from '../api/notice'
import { getProfile } from '../api/user'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const role = ref(localStorage.getItem('role') || 'USER')
const profile = ref(null)
const collapsed = ref(false)
const asideExpandedWidth = ref(176)
const asideResizing = ref(false)
const asideResizeStartX = ref(0)
const asideResizeStartWidth = ref(176)
const noticePopoverVisible = ref(false)
const noticeLoading = ref(false)
const latestNotices = ref([])
const noticeDialogVisible = ref(false)
const currentNotice = ref(null)

const asideWidth = computed(() => (collapsed.value ? '64px' : `${asideExpandedWidth.value}px`))

const roleText = computed(() => {
  if (role.value === 'ADMIN') return '管理员'
  if (role.value === 'HR') return 'HR'
  return '求职者'
})

const displayName = computed(() => {
  const p = profile.value
  return (p && (p.realName || p.username)) || '用户'
})

const avatarUrl = computed(() => {
  const p = profile.value
  if (!p || !p.avatar) return ''
  const a = p.avatar
  if (a.startsWith('http://') || a.startsWith('https://')) return a
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + a
})

const avatarFallback = computed(() => {
  const name = displayName.value || ''
  return name ? name.slice(0, 1) : 'U'
})

const activePath = computed(() => {
  return route.path === '/' ? '/home' : route.path
})

function go(path) {
  router.push(path)
}

function onSearch() {
  router.push({ path: '/home', query: { keyword: keyword.value } })
}

function onUserCommand(cmd) {
  if (cmd === 'profile') {
    go('/profile')
    return
  }
  if (cmd === 'logout') {
    onLogout()
  }
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function onAsideResizeStart(e) {
  if (collapsed.value) return
  asideResizing.value = true
  asideResizeStartX.value = e.clientX
  asideResizeStartWidth.value = asideExpandedWidth.value
  window.addEventListener('mousemove', onAsideResizing)
  window.addEventListener('mouseup', onAsideResizeEnd)
}

function onAsideResizing(e) {
  if (!asideResizing.value) return
  const delta = e.clientX - asideResizeStartX.value
  const next = asideResizeStartWidth.value + delta
  const min = 140
  const max = 280
  asideExpandedWidth.value = Math.min(max, Math.max(min, Math.round(next)))
}

function onAsideResizeEnd() {
  if (!asideResizing.value) return
  asideResizing.value = false
  window.removeEventListener('mousemove', onAsideResizing)
  window.removeEventListener('mouseup', onAsideResizeEnd)
}

function formatNoticeTime(value) {
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
    const second = value[5] || 0
    try {
      const d = new Date(year, month - 1, day, hour, minute, second)
      const pad2 = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
    } catch (e) {
      return ''
    }
  }
  try {
    const d = new Date(value)
    const pad2 = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
  } catch (e) {
    return ''
  }
}

async function loadNotices() {
  if (noticeLoading.value) return
  noticeLoading.value = true
  try {
    const list = await listNotices()
    latestNotices.value = Array.isArray(list) ? list.slice(0, 5) : []
  } catch (e) {
    latestNotices.value = []
  } finally {
    noticeLoading.value = false
  }
}

function openNotice(n) {
  currentNotice.value = n || null
  noticePopoverVisible.value = false
  noticeDialogVisible.value = true
}

async function onLogout() {
  try {
    await logout()
  } finally {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

async function loadProfile() {
  try {
    profile.value = await getProfile()
  } catch (e) {
    profile.value = null
  }
}

function onProfileChanged() {
  loadProfile()
}

onMounted(() => {
  loadProfile()
  window.addEventListener('profile-changed', onProfileChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('profile-changed', onProfileChanged)
  onAsideResizeEnd()
})
</script>

<style scoped>
.page {
  height: 100vh;
  overflow: hidden;
}

.body {
  flex: 1;
  min-height: 0;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  flex: 0 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.logo {
  font-weight: 600;
  color: #0f4c81;
  white-space: nowrap;
}

.header-center {
  flex: 1;
  max-width: 520px;
  margin-left: 40px;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.message-entry {
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  color: #303133;
}

.message-entry:hover {
  background: #f5f7fa;
}

.notice-panel-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.notice-loading,
.notice-empty {
  color: #909399;
  font-size: 13px;
  padding: 10px 0;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
}

.notice-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.notice-item:hover {
  background: #f5f7fa;
}

.notice-item-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item-time {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.notice-dialog-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
}

.user-entry:hover {
  background: #f5f7fa;
}

.user-name {
  font-size: 13px;
  color: #303133;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-caret {
  color: #909399;
  font-size: 12px;
}

.aside {
  background: #ffffff;
  border-right: 1px solid #ebeef5;
  position: relative;
  transition: width 120ms ease;
  overflow: hidden;
  overscroll-behavior: contain;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.aside-top {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.aside-toggle {
  height: 32px;
  width: 32px;
  padding: 0;
  border-radius: 6px;
}

.aside-resizer {
  position: absolute;
  top: 0;
  right: 0;
  width: 6px;
  height: 100%;
  cursor: col-resize;
  z-index: 10;
}

.aside :deep(.el-menu) {
  border-right: none;
  width: 100%;
  flex: 1;
}

.aside :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
}

.aside :deep(.el-menu-item:hover) {
  background-color: #f5f7fa;
}

.aside :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
}

.aside-top .aside-toggle {
  margin-left: auto;
  margin-right: auto;
}

.aside:not(.aside-collapsed) .aside-top {
  justify-content: flex-end;
}

.aside:not(.aside-collapsed) .aside-top .aside-toggle {
  margin-left: 0;
  margin-right: 0;
}

.main {
  background: #f5f7fa;
  overflow: auto;
  overscroll-behavior: contain;
  min-height: 0;
  scrollbar-gutter: stable;
}
</style>
