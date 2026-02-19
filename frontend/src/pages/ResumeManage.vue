<template>
  <el-card>
    <template #header>
      <div class="header">
        <div v-if="!showDetail">
          <div>简历管理</div>
          <el-button type="primary" size="small" @click="openCreate">新增简历</el-button>
        </div>
        <div v-else class="header-back">
          <el-button link @click="closeDetail">
            <el-icon><ArrowLeft /></el-icon>
            返回简历管理
          </el-button>
          <span class="detail-title">简历详情 - {{ editForm.resumeName }}</span>
        </div>
      </div>
    </template>

    <template v-if="!showDetail">
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
            <el-button size="small" type="primary" @click="openDetail(row)">查看简历</el-button>
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
    </template>

    <template v-else>
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
        <el-divider content-position="left">附件简历</el-divider>
        <el-form-item label="附件简历">
          <div class="attach-row">
            <div class="attach-info">
              <div v-if="editForm.fileUrl">
                <div class="attach-name-row">
                  <div class="attach-name">{{ editForm.fileName || editForm.fileUrl }}</div>
                  <el-popconfirm title="确认删除该附件？" @confirm="deleteAttachment">
                    <template #reference>
                      <el-icon class="attach-remove"><Close /></el-icon>
                    </template>
                  </el-popconfirm>
                </div>
                <div v-if="editForm.fileSize" class="attach-meta">{{ formatBytes(editForm.fileSize) }}</div>
              </div>
              <div v-else class="attach-empty">未上传</div>
            </div>
            <div class="attach-actions">
              <el-upload :show-file-list="false" :before-upload="beforeResumeUpload" :http-request="doUploadResume">
                <el-button size="small" :loading="uploadingResume">上传</el-button>
              </el-upload>
              <el-button size="small" :disabled="!editForm.fileUrl" @click="downloadAttachment">下载</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </template>
  </el-card>

  <el-dialog v-model="createVisible" title="新增简历" width="840px">
    <el-form :model="createForm" label-position="top">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="简历名称">
            <el-input v-model="createForm.resumeName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名">
            <el-input v-model="createForm.realName" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="城市">
            <el-input v-model="createForm.city" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="学历">
            <el-input v-model="createForm.education" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工作年限">
            <el-input v-model="createForm.workYears" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="期望岗位类型">
            <el-input v-model="createForm.expectJobType" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="期望薪资">
            <el-input v-model="createForm.expectSalary" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-switch
              v-model="createForm.status"
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
            <el-input v-model="createForm.workDesc" type="textarea" :rows="10" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="saveCreate">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="tagVisible" title="配置简历标签" width="800px">
    <div v-if="currentResume" class="tag-tip">当前简历：{{ currentResume.resumeName }}</div>
    
    <div class="tag-search-row">
      <el-input
        v-model="tagSearchKeyword"
        size="small"
        clearable
        placeholder="搜索标签添加"
        style="width: 300px"
        @input="onTagSearch"
      />
    </div>

    <div v-if="tagSearchKeyword" class="search-results">
      <div class="section-title">搜索结果</div>
      <div v-if="searchedTags.length === 0" class="empty-tip">未找到匹配的标签</div>
      <div v-else class="tag-list">
        <div v-for="t in searchedTags" :key="t.id" class="tag-item">
          <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
          <el-tag size="small" effect="plain">{{ tagTypeLabel(t.tagType) }}</el-tag>
          <el-select v-if="t.tagType === 'SKILL' && checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
            <el-option label="了解" :value="1" />
            <el-option label="熟悉" :value="2" />
            <el-option label="精通" :value="3" />
          </el-select>
        </div>
      </div>
    </div>

    <div v-else>
      <div class="tag-section">
        <div class="section-title">已配置标签</div>
        <div v-if="selectedTags.length === 0" class="empty-tip">暂未配置标签</div>
        <div v-else class="tag-list">
          <div v-for="t in selectedTags" :key="t.id" class="tag-item">
            <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            <el-tag size="small" effect="plain">{{ tagTypeLabel(t.tagType) }}</el-tag>
            <el-select v-if="t.tagType === 'SKILL' && checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
              <el-option label="了解" :value="1" />
              <el-option label="熟悉" :value="2" />
              <el-option label="精通" :value="3" />
            </el-select>
          </div>
        </div>
      </div>

      <el-collapse v-model="tagCollapseActive">
        <el-collapse-item title="技能标签" name="SKILL">
          <div v-if="skillTags.length === 0" class="empty-tip">暂无技能标签</div>
          <div v-else class="tag-list">
            <div v-for="t in skillTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
              <el-select v-if="checkedMap[t.id]" v-model="proficiencyMap[t.id]" size="small" style="width: 100px; margin-left: 8px">
                <el-option label="了解" :value="1" />
                <el-option label="熟悉" :value="2" />
                <el-option label="精通" :value="3" />
              </el-select>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item title="行业标签" name="INDUSTRY">
          <div v-if="industryTags.length === 0" class="empty-tip">暂无行业标签</div>
          <div v-else class="tag-list">
            <div v-for="t in industryTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item title="经验标签" name="EXPERIENCE">
          <div v-if="experienceTags.length === 0" class="empty-tip">暂无经验标签</div>
          <div v-else class="tag-list">
            <div v-for="t in experienceTags" :key="t.id" class="tag-item">
              <el-checkbox v-model="checkedMap[t.id]">{{ t.tagName }}</el-checkbox>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <template #footer>
      <el-button @click="tagVisible = false">取消</el-button>
      <el-button type="primary" @click="saveTags">保存标签</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Close } from '@element-plus/icons-vue'
import { listTags, searchTags } from '../api/tag'
import { deleteResume, listMyResumes, saveResume, saveResumeTags, setDefaultResume, getResumeDetail, uploadResumeAttachment, downloadMyResumeAttachment, deleteMyResumeAttachment } from '../api/resume'
import { downloadBlobResponse } from '../utils/download'

const resumes = ref([])
const tags = ref([])

const showDetail = ref(false)
const createVisible = ref(false)
const tagVisible = ref(false)
const currentResume = ref(null)
const tagSearchKeyword = ref('')
const searchedTags = ref([])
const tagCollapseActive = ref([])
const uploadingResume = ref(false)

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

const createForm = reactive({
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

const selectedTags = computed(() => {
  return tags.value.filter((t) => checkedMap[t.id])
})

const skillTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'SKILL' && !checkedMap[t.id])
})

const industryTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'INDUSTRY' && !checkedMap[t.id])
})

const experienceTags = computed(() => {
  return tags.value.filter((t) => t.tagType === 'EXPERIENCE' && !checkedMap[t.id])
})

function tagTypeLabel(type) {
  const labels = { SKILL: '技能', INDUSTRY: '行业', EXPERIENCE: '经验' }
  return labels[type] || type
}

async function onTagSearch() {
  const kw = (tagSearchKeyword.value || '').trim()
  if (!kw) {
    searchedTags.value = []
    return
  }
  const list = await searchTags({ keyword: kw, limit: 20 })
  searchedTags.value = (list || []).filter((t) => !checkedMap[t.id])
}

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
  Object.assign(createForm, {
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
  createVisible.value = true
}

function openDetail(row) {
  Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  showDetail.value = true
}

function closeDetail() {
  showDetail.value = false
}

async function save() {
  await saveResume(editForm)
  ElMessage.success('保存成功')
  showDetail.value = false
  await loadResumes()
}

async function saveCreate() {
  await saveResume(createForm)
  ElMessage.success('保存成功')
  createVisible.value = false
  await loadResumes()
}

function beforeResumeUpload(file) {
  if (!editForm.id) {
    ElMessage.error('请先保存简历后再上传附件')
    return false
  }
  const name = (file && file.name) || ''
  const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : ''
  const allowed = ['pdf', 'jpg', 'jpeg', 'png']
  if (!allowed.includes(ext)) {
    ElMessage.error('附件仅支持 pdf/jpg/jpeg/png')
    return false
  }
  const max = 10 * 1024 * 1024
  if (file.size > max) {
    ElMessage.error('附件大小不能超过10MB')
    return false
  }
  return true
}

async function doUploadResume(options) {
  uploadingResume.value = true
  try {
    const res = await uploadResumeAttachment(editForm.id, options.file)
    Object.assign(editForm, res || {})
    ElMessage.success('附件已上传')
    options.onSuccess(res)
  } catch (e) {
    options.onError(e)
  } finally {
    uploadingResume.value = false
  }
}

async function downloadAttachment() {
  if (!editForm.id) return
  const resp = await downloadMyResumeAttachment(editForm.id)
  downloadBlobResponse(resp, editForm.fileName || 'resume')
}

async function deleteAttachment() {
  if (!editForm.id) return
  const res = await deleteMyResumeAttachment(editForm.id)
  Object.assign(editForm, res || {})
  ElMessage.success('附件已删除')
}

function formatBytes(value) {
  const num = Number(value)
  if (!num || Number.isNaN(num)) return ''
  if (num < 1024) return `${num} B`
  const kb = num / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  const mb = kb / 1024
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  const gb = mb / 1024
  return `${gb.toFixed(1)} GB`
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
  tagSearchKeyword.value = ''
  searchedTags.value = []
  tagCollapseActive.value = []
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

.header > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-back {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-title {
  font-weight: 600;
  color: #303133;
}

.tag-tip {
  font-size: 12px;
  color: #606266;
  margin-bottom: 12px;
}

.tag-search-row {
  margin-bottom: 12px;
}

.search-results {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  font-size: 14px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.empty-tip {
  color: #909399;
  font-size: 13px;
}

.tag-section {
  margin-bottom: 16px;
}

.attach-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attach-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attach-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.attach-name {
  font-size: 13px;
  color: #303133;
}

.attach-remove {
  cursor: pointer;
  color: #909399;
}

.attach-remove:hover {
  color: #f56c6c;
}

.attach-meta {
  font-size: 12px;
  color: #909399;
}

.attach-empty {
  font-size: 13px;
  color: #909399;
}

.attach-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
