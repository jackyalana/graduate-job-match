<template>
  <div class="wrap">
    <el-card class="card">
      <div class="title">注册账号</div>
      <el-alert v-if="isDemo" title="本地演示注册：请使用合成信息，邮箱不会验证" type="info" :closable="false" />

      <div class="role-switch">
        <el-radio-group v-model="form.role">
          <el-radio-button label="USER">我要找工作</el-radio-button>
          <el-radio-button label="HR">我要招聘</el-radio-button>
        </el-radio-group>
      </div>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item v-if="form.role === 'HR'" label="公司名称">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="!isDemo" label="验证码">
          <el-input v-model="form.code" placeholder="请输入验证码">
            <template #append>
              <el-button :disabled="cooldown > 0" @click="onSendRegisterCode">
                {{ cooldown > 0 ? `${cooldown}s` : '发送' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <el-button type="primary" class="btn" @click="onRegister">注册</el-button>
      <el-button class="btn" @click="goLogin">返回登录</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendEmailCode } from '../api/auth'

const isDemo = import.meta.env.VITE_DEMO === 'true'
const router = useRouter()
const form = reactive({ role: 'USER', username: '', password: '', companyName: '', email: '', code: '' })
const cooldown = ref(0)
let timer = null

function startCooldown() {
  cooldown.value = 30
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      cooldown.value = 0
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function onSendRegisterCode() {
  if (!form.email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  if (cooldown.value > 0) return
  try {
    await sendEmailCode(form.email, 'REGISTER')
    ElMessage.success('验证码已发送')
    startCooldown()
  } catch (e) {
    return
  }
}

async function onRegister() {
  await register(form)
  ElMessage.success(isDemo ? '演示注册成功，邮箱未验证' : '注册成功，请登录')
  router.push('/login')
}

function goLogin() {
  router.push('/login')
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
}

.card {
  width: 420px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 14px;
}

.role-switch {
  margin-bottom: 12px;
}

.btn {
  width: 100%;
  margin-top: 10px;
}

.btn + .btn {
  margin-left: 0;
}
</style>
