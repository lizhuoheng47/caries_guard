<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { kbApi } from '../../api/kb'

const rebuildJobs = ref<any[]>([])
onMounted(async () => {
  rebuildJobs.value = await kbApi.rebuildJobs()
})
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>Rebuild Job</h2>
      <el-table :data="rebuildJobs">
        <el-table-column prop="rebuild_job_no" label="任务编号" min-width="220" />
        <el-table-column prop="rebuild_status_code" label="状态" width="120" />
        <el-table-column prop="knowledge_version" label="知识版本" width="140" />
        <el-table-column prop="chunk_count" label="Chunk 数" width="120" />
        <el-table-column prop="error_message" label="错误信息" min-width="280" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
</style>
