<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ragApi } from '../../api/rag'

const graphLogs = ref<any[]>([])
onMounted(async () => {
  const requests = await ragApi.requests()
  if (requests.length > 0) {
    graphLogs.value = await ragApi.graphLogs(requests[0].request_no)
  }
})
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>图谱证据</h2>
      <el-table :data="graphLogs">
        <el-table-column prop="cypher_template_code" label="Cypher 模板" />
        <el-table-column prop="score" label="分数" />
        <el-table-column prop="result_path_json" label="路径 JSON" min-width="360">
          <template #default="{ row }">
            <pre>{{ row.result_path_json }}</pre>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
pre { white-space: pre-wrap; }
</style>
