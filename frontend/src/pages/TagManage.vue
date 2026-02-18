<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>标签库管理</div>
        <el-button type="primary" size="small" @click="openCreate">新增标签</el-button>
      </div>
    </template>

    <div class="filters">
      <el-input
        v-model="query.keyword"
        size="small"
        clearable
        placeholder="搜索标签名称"
        style="width: 200px"
        @keyup.enter="load"
      />
      <el-select v-model="query.tagType" clearable placeholder="类型" style="width: 160px" @change="load">
        <el-option label="技能(SKILL)" value="SKILL" />
        <el-option label="行业(INDUSTRY)" value="INDUSTRY" />
        <el-option label="经验(EXPERIENCE)" value="EXPERIENCE" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="load">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button size="small" @click="load">搜索</el-button>
    </div>

    <el-table :data="tags" size="small" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="tagName" label="标签名称" min-width="160" />
      <el-table-column prop="tagType" label="类型" width="140">
        <template #default="{ row }">
          {{ tagTypeLabel(row.tagType) }}
        </template>
      </el-table-column>
      <el-table-column prop="baseWeight" label="基础权重" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="info">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该标签？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

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

  <el-dialog v-model="editVisible" :title="form.id ? '编辑标签' : '新增标签'" width="520px">
    <el-form :model="form" label-position="top">
      <el-form-item label="标签名称">
        <el-input v-model="form.tagName" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.tagType" style="width: 200px">
          <el-option label="技能(SKILL)" value="SKILL" />
          <el-option label="行业(INDUSTRY)" value="INDUSTRY" />
          <el-option label="经验(EXPERIENCE)" value="EXPERIENCE" />
        </el-select>
      </el-form-item>
      <el-form-item label="基础权重">
        <el-input-number v-model="form.baseWeight" :min="1" :max="100" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminDeleteTag, adminListTagsPage, adminSaveTag } from '../api/tag'

const tags = ref([])
const editVisible = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const query = reactive({ tagType: '', status: '', keyword: '' })
const form = reactive({ id: null, tagName: '', tagType: 'SKILL', baseWeight: 1, status: 1 })

function tagTypeLabel(type) {
  const labels = { SKILL: '技能', INDUSTRY: '行业', EXPERIENCE: '经验' }
  return labels[type] || type
}

async function load() {
  const res = await adminListTagsPage({
    tagType: query.tagType || null,
    status: query.status == null || query.status === '' ? null : query.status,
    keyword: query.keyword || null,
    page: page.value,
    pageSize: pageSize.value
  })
  tags.value = res.list || []
  total.value = res.total || 0
}

function openCreate() {
  Object.assign(form, { id: null, tagName: '', tagType: 'SKILL', baseWeight: 1, status: 1 })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  editVisible.value = true
}

async function save() {
  await adminSaveTag(form)
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

async function remove(row) {
  await adminDeleteTag(row.id)
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
