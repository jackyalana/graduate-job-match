<template>
  <el-card>
    <template #header>
      <div class="header">
        <div>简历管理</div>
        <el-button type="primary" size="small" @click="openCreate">新增简历</el-button>
      </div>
    </template>

    <el-table :data="resumes" size="small" style="width: 100%">
      <el-table-column prop="resumeName" label="简历名称" min-width="160" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="education" label="学历" width="110" />
      <el-table-column prop="isDefault" label="默认" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">有效</el-tag>
          <el-tag v-else type="info">无效</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="查看简历" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">查看简历</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openTags(row)">标签</el-button>
          <el-button size="small" @click="setDefault(row)">设为默认</el-button>
          <el-popconfirm title="确定删除该简历？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editVisible" :title="editForm.id ? '查看简历' : '新增简历'" width="840px">
    <el-form :model="editForm" label-position="top">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="简历名称">
            <el-input v-model="editForm.resumeName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名">
            <el-input v-model="editForm.realName" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="城市">
            <el-input v-model="editForm.city" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="学历">
            <el-input v-model="editForm.education" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工作年限">
            <el-input v-model="editForm.workYears" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="期望岗位类型">
            <el-input v-model="editForm.expectJobType" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="期望薪资">
            <el-input v-model="editForm.expectSalary" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-switch
              v-model="editForm.status"
              :active-value="1"
              :inactive-value="0"
              active-text="有效"
              inactive-text="无效"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="工作描述">
            <el-input v-model="editForm.workDesc" type="textarea" :rows="10" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="tagVisible" title="配置简历标签" width="720px">
    <div v-if="currentResume" class="tag-tip">当前简历：{{ currentResume.resumeName }}</div>
    <div class="tag-search-row">
      <el-input
        v-model="tagKeyword"
        size="small"
        clearable
        placeholder="搜索标签"
        style="width: 240px"
        @input="filterTags"
      />
    </div>
    <el-table :data="filteredTags" size="small" style="width: 100%; margin-top: 8px" max-height="400">
      <el-table-column label="选择" width="60">
        <template #default="{ row }">
          <el-checkbox v-model="checkedMap[row.id]" />
        </template>
      </el-table-column>
      <el-table-column prop="tagName" label="标签" min-width="160" />
      <el-table-column prop="tagType" label="类型" width="120">
        <template #default="{ row }">
          {{ tagTypeLabel(row.tagType) }}
        </template>
      </el-table-column>
      <el-table-column label="熟练度(技能)" width="160">
        <template #default="{ row }">
          <el-select v-if="row.tagType === 'SKILL'" v-model="proficiencyMap[row.id]" style="width: 120px">
            <el-option label="了解(1)" :value="1" />
            <el-option label="熟悉(2)" :value="2" />
            <el-option label="精通(3)" :value="3" />
          </el-select>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="tagVisible = false">取消</el-button>
      <el-button type="primary" @click="saveTags">保存标签</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTags } from '../api/tag'
import { deleteResume, listMyResumes, saveResume, saveResumeTags, setDefaultResume, getResumeDetail } from '../api/resume'

const resumes = ref([])
const tags = ref([])

const editVisible = ref(false)
const tagVisible = ref(false)
const currentResume = ref(null)
const tagKeyword = ref('')

const editForm = reactive({
  id: null,
  resumeName: '',
  realName: '',
  city: '',
  education: '',
  workYears: '',
  expectJobType: '',
  expectSalary: '',
  workDesc: '',
  status: 1
})

const checkedMap = reactive({})
const proficiencyMap = reactive({})

const filteredTags = computed(() => {
  const kw = (tagKeyword.value || '').trim().toLowerCase()
  if (!kw) return tags.value
  return tags.value.filter((t) => t.tagName.toLowerCase().includes(kw))
})

function tagTypeLabel(type) {
  const labels = { SKILL: '技能', INDUSTRY: '行业', EXPERIENCE: '经验' }
  return labels[type] || type
}

function filterTags() {}

async function loadResumes() {
  resumes.value = await listMyResumes()
}

async function loadTags() {
  tags.value = await listTags()
  tags.value.forEach((t) => {
    if (typeof proficiencyMap[t.id] === 'undefined') proficiencyMap[t.id] = 1
    if (typeof checkedMap[t.id] === 'undefined') checkedMap[t.id] = false
  })
}

function openCreate() {
  Object.assign(editForm, {
    id: null,
    resumeName: '',
    realName: '',
    city: '',
    education: '',
    workYears: '',
    expectJobType: '',
    expectSalary: '',
    workDesc: '',
    status: 1
  })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  editVisible.value = true
}

async function save() {
  await saveResume(editForm)
  ElMessage.success('保存成功')
  editVisible.value = false
  await loadResumes()
}

async function remove(row) {
  await deleteResume(row.id)
  ElMessage.success('已删除')
  await loadResumes()
}

async function setDefault(row) {
  await setDefaultResume(row.id)
  ElMessage.success('已设置默认简历')
  await loadResumes()
}

async function openTags(row) {
  currentResume.value = row
  tagKeyword.value = ''
  Object.keys(checkedMap).forEach((k) => (checkedMap[k] = false))
  const detail = await getResumeDetail(row.id)
  ;(detail.tags || []).forEach((t) => {
    checkedMap[t.tagId] = true
    if (t.proficiency != null) proficiencyMap[t.tagId] = t.proficiency
  })
  tagVisible.value = true
}

async function saveTags() {
  const items = tags.value
    .filter((t) => checkedMap[t.id])
    .map((t) => ({
      tagId: t.id,
      proficiency: t.tagType === 'SKILL' ? proficiencyMap[t.id] : null
    }))
  await saveResumeTags(currentResume.value.id, items)
  ElMessage.success('标签已保存')
  tagVisible.value = false
}

onMounted(async () => {
  await loadResumes()
  await loadTags()
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tag-tip {
  font-size: 12px;
  color: #606266;
}

.tag-search-row {
  margin-top: 8px;
}
</style>
