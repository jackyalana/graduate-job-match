<template>
  <el-card><template #header>基础统计</template>
    <p>当前已审核在招岗位：{{ data.openJobs ?? '-' }}</p>
    <p>累计投递记录：{{ data.applications ?? '-' }}</p>
    <p>投递状态分布：<span v-for="(count,status) in data.applicationStatusCounts" :key="status">{{ labels[status] || status }} {{ count }}；</span></p>
    <p>在招岗位类别分布：<span v-for="(count,id) in data.openJobsByCategory" :key="id">类别 #{{ id }} {{ count }}；</span></p>
    <small>统计口径：累计投递；当前公开且有效的在招岗位。演示数据为合成数据。</small>
  </el-card>
</template>
<script setup>
import { ref,onMounted } from 'vue'
import request from '../api/request'
const data=ref({})
const labels=['已投递','已查看','面试中','已拒绝','已录用','已接受','已婉拒','已撤回']
onMounted(async()=>{data.value=await request.get('/api/admin/statistics')})
</script>
