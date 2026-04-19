<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ragApi } from '../../api/rag'

const route = useRoute()
const requestNo = ref<string>('')
const graphLogs = ref<any[]>([])

const asArray = (value: unknown): any[] => (Array.isArray(value) ? value : [])

const nodeCount = (row: any) => {
  const path = row.result_path_json || row.resultPathJson || {}
  return asArray(path.nodes).length
}

const edgeCount = (row: any) => {
  const path = row.result_path_json || row.resultPathJson || {}
  return asArray(path.edges).length
}

const citationSummary = (row: any) => {
  const parts = [row.doc_title || row.docTitle, row.doc_version || row.docVersion, row.chunk_id || row.chunkId]
    .filter(Boolean)
    .map(String)
  return parts.length ? parts.join(' / ') : '-'
}

const load = async () => {
  requestNo.value = String(route.params.requestNo || route.query.requestNo || '')
  if (!requestNo.value) {
    const requests = await ragApi.requests()
    requestNo.value = requests[0]?.request_no || requests[0]?.requestNo || ''
  }
  if (requestNo.value) {
    graphLogs.value = await ragApi.graphLogs(requestNo.value)
  }
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>图谱证据</h2>
      <p>RequestNo: {{ requestNo || '-' }}</p>
      <el-table :data="graphLogs">
        <el-table-column prop="cypher_template_code" label="Cypher 模板" min-width="180" />
        <el-table-column prop="score" label="分数" width="100" />
        <el-table-column label="Path Summary" width="140">
          <template #default="{ row }">
            <div>{{ nodeCount(row) }} nodes</div>
            <div>{{ edgeCount(row) }} edges</div>
          </template>
        </el-table-column>
        <el-table-column label="Related Evidence" min-width="220">
          <template #default="{ row }">
            <div>{{ citationSummary(row) }}</div>
            <div>graphPathId: {{ row.graph_path_id || row.graphPathId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="result_path_json" label="路径 JSON" min-width="360">
          <template #default="{ row }">
            <pre>{{ row.result_path_json || row.resultPathJson }}</pre>
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
