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
          <el-table-column prop="createdAt" label="申请时间" width="180" />
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
          <el-table-column prop="createdAt" label="提交时间" width="180" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="decisionJob(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="decisionJob(row, false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminDecisionHr, adminDecisionJob, adminListPendingHr, adminListPendingJob } from '../api/job'

const tab = ref('hr')
const hrList = ref([])
const jobList = ref([])

async function load() {
  if (tab.value === 'hr') {
    hrList.value = await adminListPendingHr()
    return
  }
  jobList.value = await adminListPendingJob()
}

async function decisionHr(row, pass) {
  await adminDecisionHr(row.id, pass)
  ElMessage.success('已处理')
  await load()
}

async function decisionJob(row, pass) {
  await adminDecisionJob(row.id, pass)
  ElMessage.success('已处理')
  await load()
}

onMounted(load)
</script>
