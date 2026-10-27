<template>
  <el-card>
    <template #header>个人中心</template>

    <div class="avatar-row">
      <el-avatar :size="64" :src="avatarSrc">
        {{ avatarFallback }}
      </el-avatar>
      <div class="avatar-meta">
        <div class="avatar-name">{{ me ? me.username : '' }}</div>
        <el-upload
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="doUploadAvatar"
        >
          <el-button size="small" :loading="uploading">上传/更换头像</el-button>
        </el-upload>
      </div>
    </div>

    <el-descriptions v-if="me" class="base-info" :column="1" border>
      <el-descriptions-item label="用户ID">{{ me.id }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ me.username }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ roleLabel }}</el-descriptions-item>
      <el-descriptions-item label="邮箱验证">{{ me.emailVerified ? '已验证' : '未验证' }}</el-descriptions-item>
      <el-descriptions-item v-if="me.role === 'HR'" label="公司名称">{{ profile?.companyName || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <el-form :model="profileForm" label-position="top" style="max-width: 520px">
      <el-form-item v-if="showRealName" label="姓名">
        <el-input v-model="profileForm.realName" />
      </el-form-item>
      <el-form-item v-if="showPhone" label="手机号">
        <el-input v-model="profileForm.phone" />
      </el-form-item>
      <el-form-item v-if="showSchool" label="学校">
        <el-input v-model="profileForm.school" />
      </el-form-item>
      <el-form-item v-if="showGraduateDate" label="毕业时间">
        <el-date-picker v-model="profileForm.graduateDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-button type="primary" @click="saveProfile">保存资料</el-button>
    </el-form>

    <el-divider />

    <el-collapse v-model="pwdActive" style="max-width: 520px">
      <el-collapse-item title="修改密码" name="pwd">
        <el-form :model="pwdForm" label-position="top">
          <el-form-item label="旧密码">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" @click="savePassword">修改密码</el-button>
        </el-form>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword, getMe, getProfile, updateProfile, uploadAvatar } from '../api/user'

const router = useRouter()
const me = ref(null)
const profile = ref(null)
const uploading = ref(false)
const profileForm = reactive({ realName: '', phone: '', school: '', graduateDate: '', companyName: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdActive = ref([])

const roleLabel = computed(() => {
  const r = me.value && me.value.role
  if (r === 'USER') return '求职者'
  if (r === 'HR') return 'HR'
  if (r === 'ADMIN') return '管理员'
  return r || ''
})

const showRealName = computed(() => {
  const r = me.value && me.value.role
  return r === 'USER' || r === 'HR'
})

const showPhone = computed(() => {
  const r = me.value && me.value.role
  return r === 'USER' || r === 'HR'
})

const showSchool = computed(() => {
  const r = me.value && me.value.role
  return r === 'USER'
})

const showGraduateDate = computed(() => {
  const r = me.value && me.value.role
  return r === 'USER'
})

const avatarFallback = computed(() => {
  const name = (me.value && me.value.username) || 'U'
  return name.slice(0, 1)
})

const avatarSrc = computed(() => {
  const p = profile.value
  if (!p || !p.avatar) return ''
  const a = p.avatar
  if (a.startsWith('http://') || a.startsWith('https://')) return a
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + a
})

async function load() {
  me.value = await getMe()
  profile.value = await getProfile()
  profileForm.realName = profile.value.realName || ''
  profileForm.phone = profile.value.phone || ''
  profileForm.school = profile.value.school || ''
  profileForm.graduateDate = profile.value.graduateDate || ''
  profileForm.companyName = profile.value.companyName || ''
}

async function saveProfile() {
  const r = me.value && me.value.role
  const payload = {}
  if (r === 'USER') {
    payload.realName = profileForm.realName
    payload.phone = profileForm.phone
    payload.school = profileForm.school
    payload.graduateDate = profileForm.graduateDate || null
  }
  if (r === 'HR') {
    payload.realName = profileForm.realName
    payload.phone = profileForm.phone
  }
  await updateProfile(payload)
  window.dispatchEvent(new Event('profile-changed'))
  ElMessage.success('资料已保存')
}

async function savePassword() {
  await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
  ElMessage.success('密码已修改，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  router.push('/login')
}

function beforeAvatarUpload(file) {
  const isImage = ['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)
  if (!isImage) {
    ElMessage.error('仅支持 JPG/PNG 图片')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 <= 2
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function doUploadAvatar(options) {
  uploading.value = true
  try {
    const url = await uploadAvatar(options.file)
    if (!profile.value) profile.value = {}
    profile.value.avatar = url
    window.dispatchEvent(new Event('profile-changed'))
    ElMessage.success('头像已更新')
    options.onSuccess(url)
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-name {
  font-weight: 600;
  color: #303133;
}

.base-info :deep(.el-descriptions__label) {
  width: 64px;
  min-width: 64px;
  white-space: nowrap;
}
</style>
