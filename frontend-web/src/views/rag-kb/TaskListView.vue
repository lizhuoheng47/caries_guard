<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ragApi } from '../../api/rag'

const ingestJobs = ref<any[]>([])
const rebuildJobs = ref<any[]>([])
onMounted(async () => {
  ingestJobs.value = await ragApi.ingestJobs()
  rebuildJobs.value = await ragApi.rebuildJobs()
})
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>导入任务</h2>
      <el-table :data="ingestJobs">
        <el-table-column prop="ingest_job_no" label="任务编号" />
        <el-table-column prop="ingest_status_code" label="状态" />
        <el-table-column prop="current_step_code" label="当前步骤" />
        <el-table-column prop="error_message" label="错误信息" min-width="240" />
      </el-table>
    </div>
    <div class="glass-card panel">
      <h2>重建任务</h2>
      <el-table :data="rebuildJobs">
        <el-table-column prop="rebuild_job_no" label="任务编号" />
        <el-table-column prop="rebuild_status_code" label="状态" />
        <el-table-column prop="knowledge_version" label="知识版本" />
        <el-table-column prop="error_message" label="错误信息" min-width="240" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
</style>
