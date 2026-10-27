<template>
  <el-card>
    <template #header>求职画像</template>
    <p>请填写真实的求职偏好；未填写的学历和毕业年份不能用于受限岗位推荐。</p>
    <el-form :model="form" label-position="top" style="max-width: 560px" v-loading="loading">
      <el-form-item label="学校"><el-input v-model="form.school" /></el-form-item>
      <el-form-item label="专业代码"><el-input v-model="form.majorCode" /></el-form-item>
      <el-form-item label="学历">
        <el-select v-model="form.educationRank" clearable placeholder="请选择">
          <el-option label="专科" :value="1" /><el-option label="本科" :value="2" />
          <el-option label="硕士" :value="3" /><el-option label="博士" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="毕业年份"><el-input-number v-model="form.graduationYear" :min="1900" :max="2100" /></el-form-item>
      <el-form-item label="期望岗位类别">
        <el-select v-model="form.expectedCategoryId" clearable placeholder="请选择">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="期望最低月薪（元）"><el-input-number v-model="form.salaryMin" :min="0" /></el-form-item>
      <el-form-item label="期望城市代码（以逗号分隔）"><el-input v-model="cities" placeholder="例如：上海, 杭州" /></el-form-item>
      <el-button type="primary" @click="save">保存画像</el-button>
    </el-form>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
const loading=ref(false), categories=ref([]), cities=ref('')
const form=reactive({school:null, majorCode:null, educationRank:null, graduationYear:null, expectedCategoryId:null, salaryMin:null})
onMounted(async () => {
  loading.value=true
  try {
    const [profile, list]=await Promise.all([request.get('/api/student/profile'),request.get('/api/public/job/categories')])
    Object.keys(form).forEach(key => { form[key]=profile[key] ?? null })
    cities.value=(profile.cityCodes || []).join(', '); categories.value=list || []
  } finally { loading.value=false }
})
async function save() {
  const result=await request.put('/api/student/profile', {...form, cityCodes:cities.value.split(',').map(x=>x.trim()).filter(Boolean)})
  cities.value=(result.cityCodes || []).join(', ')
  ElMessage.success('画像已保存')
}
</script>
