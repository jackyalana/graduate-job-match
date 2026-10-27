<template>
  <el-card>
    <template #header>审核管理</template>

    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="HR审核" name="hr">
        <el-table :data="hrList" size="small" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="companyName" label="公司名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="email" label="邮箱" min-width="200" />
          <el-table-column label="申请时间" width="180"><template #default="{ row }">{{ formatShanghai(row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="decisionHr(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="decisionHr(row, false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="岗位审核" name="job">
        <el-table :data="jobList" size="small" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="jobName" label="岗位名称" min-width="160" />
          <el-table-column prop="city" label="城市" width="100" />
          <el-table-column prop="jobType" label="类型" width="100" />
          <el-table-column prop="salaryRange" label="薪资" width="130" />
          <el-table-column label="提交时间" width="180"><template #default="{ row }">{{ formatShanghai(row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <el-button size="small" @click="openJobDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="jobDetailVisible" title="岗位详情" width="760px">
    <el-descriptions v-if="jobDetail" :column="2" border>
      <el-descriptions-item label="岗位ID">{{ jobDetail.id }}</el-descriptions-item>
      <el-descriptions-item label="HR账号">{{ jobDetail.hrUsername || jobDetail.hrId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="岗位名称" :span="2">{{ jobDetail.jobName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="公司名称" :span="2">{{ jobDetail.companyName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ jobDetail.city || '-' }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ jobDetail.jobType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="薪资" :span="2">{{ jobDetail.salaryRange || '-' }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ jobDetail.version }}</el-descriptions-item>
      <el-descriptions-item label="类别 ID">{{ jobDetail.categoryId ?? '未设置' }}</el-descriptions-item>
      <el-descriptions-item label="城市代码">{{ jobDetail.cityCode || '未设置' }}</el-descriptions-item>
      <el-descriptions-item label="结构化薪资">{{ jobDetail.salaryNegotiable ? '面议' : `${jobDetail.salaryMin ?? '未设置'} - ${jobDetail.salaryMax ?? '未设置'}` }}</el-descriptions-item>
      <el-descriptions-item label="最低学历等级">{{ jobDetail.educationMinRank ?? '不限' }}</el-descriptions-item>
      <el-descriptions-item label="毕业年份">{{ jobDetail.graduateYearMin ?? '不限' }} - {{ jobDetail.graduateYearMax ?? '不限' }}</el-descriptions-item>
      <el-descriptions-item label="接受应届生">{{ jobDetail.acceptFreshGraduate ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="截止时间">{{ formatShanghai(jobDetail.deadline) || '未设置' }}</el-descriptions-item>
      <el-descriptions-item label="技能及要求标签" :span="2">{{ (jobDetail.tags || []).map(t => `${t.tagName || t.tagId}（${t.tagType || '未知'}，熟练度 ${t.proficiency ?? '不适用'}，权重 ${t.weight ?? '未设置'}）`).join('、') || '无' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <span v-if="jobDetail.status === 1">招聘中</span>
        <span v-else-if="jobDetail.status === 0">下架</span>
        <span v-else>{{ jobDetail.status == null ? '-' : jobDetail.status }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="审核">
        <span v-if="jobDetail.auditStatus === 0">待审核</span>
        <span v-else-if="jobDetail.auditStatus === 1">通过</span>
        <span v-else-if="jobDetail.auditStatus === 2">拒绝</span>
        <span v-else>{{ jobDetail.auditStatus == null ? '-' : jobDetail.auditStatus }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatShanghai(jobDetail.createdAt) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatShanghai(jobDetail.updatedAt) || '-' }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="jobDetail && jobDetail.description" style="margin-top: 12px">
      <div style="font-weight: 600; margin-bottom: 6px">岗位描述</div>
      <el-input :model-value="jobDetail.description" type="textarea" :rows="10" readonly />
    </div>
    <template #footer>
      <el-button type="success" @click="decisionJob(jobDetail, true)">通过此版本</el-button>
      <el-button type="danger" @click="decisionJob(jobDetail, false)">拒绝此版本</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { formatShanghai } from '../utils/time'
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDecisionHr, adminDecisionJob, adminGetJobDetail, adminListPendingHr, adminListPendingJob } from '../api/job'

const tab = ref('hr')
const hrList = ref([])
const jobList = ref([])
const jobDetailVisible = ref(false)
const jobDetail = ref(null)

async function load() {
  if (tab.value === 'hr') {
    hrList.value = await adminListPendingHr()
    return
  }
  jobList.value = await adminListPendingJob()
}

async function decisionHr(row, pass) {
  const reason = pass ? null : (await ElMessageBox.prompt('请输入拒绝原因', '审核原因', { inputValidator: v => !!v?.trim() || '请填写原因' })).value
  await adminDecisionHr(row.id, pass, reason)
  ElMessage.success('已处理')
  await load()
}

async function decisionJob(row, pass) {
  const reason = pass ? null : (await ElMessageBox.prompt('请输入拒绝原因', '审核原因', { inputValidator: v => !!v?.trim() || '请填写原因' })).value
  await adminDecisionJob(row.id, pass, row.version, reason)
  ElMessage.success('已处理')
  jobDetailVisible.value = false
  await load()
}

async function openJobDetail(row) {
  jobDetail.value = await adminGetJobDetail(row.id)
  jobDetailVisible.value = true
}

onMounted(load)
</script>
