<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>账号管理</div>
      </div>
    </template>

    <div class="filters">
      <el-input
        v-model="query.keyword"
        size="small"
        clearable
        placeholder="搜索用户名 / 邮箱 / 姓名 / 手机 / 公司"
        style="width: 260px"
        @keyup.enter="load"
      />
      <el-select v-model="query.role" clearable placeholder="角色" style="width: 150px" @change="onFilterChange">
        <el-option label="求职者(USER)" value="USER" />
        <el-option label="HR" value="HR" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="onFilterChange">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select
        v-model="query.auditStatus"
        clearable
        placeholder="审核状态"
        style="width: 160px"
        @change="onFilterChange"
      >
        <el-option label="待审核" :value="0" />
        <el-option label="通过" :value="1" />
        <el-option label="拒绝" :value="2" />
      </el-select>
      <el-button size="small" @click="load">搜索</el-button>
    </div>

    <div class="table-wrap">
      <el-table :data="users" size="small" style="width: 100%" table-layout="fixed" :fit="true">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" :src="avatarUrl(row.avatar)">
                {{ avatarFallback(row.username) }}
              </el-avatar>
              <div class="user-cell-text">
                <div class="user-cell-name">{{ row.realName || row.username || '-' }}</div>
                <div class="user-cell-sub">{{ row.username || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="120" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'HR'" type="warning">HR</el-tag>
            <el-tag v-else type="info">USER</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核" width="80">
          <template #default="{ row }">
            <span v-if="row.auditStatus == null">-</span>
            <el-tag v-else-if="row.auditStatus === 0" type="info">待审</el-tag>
            <el-tag v-else-if="row.auditStatus === 1" type="success">通过</el-tag>
            <el-tag v-else type="danger">拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="warning" @click="setStatus(row, 0)">禁用</el-button>
            <el-button v-else size="small" type="success" @click="setStatus(row, 1)">启用</el-button>
            <el-popconfirm title="确定注销并删除该账号？" @confirm="remove(row)">
              <template #reference>
                <el-button size="small" type="danger">注销</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminDeleteUser, adminListUsersPage, adminUpdateUserStatus } from '../api/adminUser'

const users = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const query = reactive({ role: '', status: '', auditStatus: '', keyword: '' })

function avatarUrl(avatar) {
  if (!avatar) return ''
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return base + avatar
}

function avatarFallback(username) {
  const name = username || ''
  return name ? name.slice(0, 1) : 'U'
}

function onFilterChange() {
  page.value = 1
  load()
}

async function load() {
  const res = await adminListUsersPage({
    role: query.role || null,
    status: query.status == null || query.status === '' ? null : query.status,
    auditStatus: query.auditStatus == null || query.auditStatus === '' ? null : query.auditStatus,
    keyword: query.keyword || null,
    page: page.value,
    pageSize: pageSize.value
  })
  users.value = res.list || []
  total.value = res.total || 0
}

async function setStatus(row, status) {
  await adminUpdateUserStatus(row.id, status)
  ElMessage.success(status === 1 ? '已启用' : '已禁用')
  await load()
}

async function remove(row) {
  await adminDeleteUser(row.id)
  ElMessage.success('已注销')
  await load()
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.table-wrap {
  width: 100%;
  overflow-x: hidden;
}

.table-wrap :deep(.el-table__header-wrapper) {
  overflow-x: hidden;
}

.table-wrap :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.table-wrap :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.user-cell-name {
  font-weight: 600;
}

.user-cell-sub {
  font-size: 12px;
  color: #999;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
