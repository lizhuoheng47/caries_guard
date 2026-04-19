<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { kbApi } from '../../api/kb'

const ingestJobs = ref<any[]>([])
onMounted(async () => {
  ingestJobs.value = await kbApi.ingestJobs()
})
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>Ingest Job</h2>
      <el-table :data="ingestJobs">
        <el-table-column prop="ingest_job_no" label="任务编号" min-width="220" />
        <el-table-column prop="ingest_status_code" label="状态" width="120" />
        <el-table-column prop="current_step_code" label="当前步骤" width="160" />
        <el-table-column prop="trace_id" label="TraceId" min-width="220" />
        <el-table-column prop="error_message" label="错误信息" min-width="280" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
</style>
