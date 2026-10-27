<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>公告管理</div>
        <el-button type="primary" size="small" @click="openCreate">新增公告</el-button>
      </div>
    </template>

    <div class="filters">
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="load">
        <el-option label="上线" :value="1" />
        <el-option label="下线" :value="0" />
      </el-select>
    </div>

    <el-table :data="notices" size="small" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">上线</el-tag>
          <el-tag v-else type="info">下线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180"><template #default="{ row }">{{ formatShanghai(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该公告？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editVisible" :title="form.id ? '编辑公告' : '新增公告'" width="640px">
    <el-form :model="form" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" :rows="6" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 140px">
          <el-option label="上线(1)" :value="1" />
          <el-option label="下线(0)" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { formatShanghai } from '../utils/time'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminDeleteNotice, adminListNotices, adminSaveNotice } from '../api/notice'

const notices = ref([])
const editVisible = ref(false)

const query = reactive({ status: '' })
const form = reactive({ id: null, title: '', content: '', status: 1 })

async function load() {
  notices.value = await adminListNotices(query.status == null || query.status === '' ? null : query.status)
}

function openCreate() {
  Object.assign(form, { id: null, title: '', content: '', status: 1 })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  editVisible.value = true
}

async function save() {
  await adminSaveNotice(form)
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

async function remove(row) {
  await adminDeleteNotice(row.id)
  ElMessage.success('已删除')
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
}
</style>
