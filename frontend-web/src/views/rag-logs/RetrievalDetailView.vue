<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ragApi } from '../../api/rag'

const route = useRoute()
const requestNo = ref<string>('')
const retrievalLogs = ref<any[]>([])
const fusionLogs = ref<any[]>([])
const rerankLogs = ref<any[]>([])
const llmLogs = ref<any[]>([])

const load = async (target?: string) => {
  requestNo.value = target || String(route.params.requestNo || route.query.requestNo || '')
  if (!requestNo.value) return
  retrievalLogs.value = await ragApi.retrievalLogs(requestNo.value)
  fusionLogs.value = await ragApi.fusionLogs(requestNo.value)
  rerankLogs.value = await ragApi.rerankLogs(requestNo.value)
  llmLogs.value = await ragApi.llmLogs(requestNo.value)
}

onMounted(() => load())
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <div class="toolbar">
        <h2>Retrieval / Fusion / Rerank 明细</h2>
        <el-input v-model="requestNo" placeholder="输入 requestNo" style="max-width: 320px" />
        <el-button type="primary" @click="load(requestNo)">加载</el-button>
      </div>
      <h3>Retrieval</h3>
      <el-table :data="retrievalLogs">
        <el-table-column prop="rank_no" label="Rank" width="80" />
        <el-table-column prop="retrieval_channel_code" label="Channel" width="120" />
        <el-table-column prop="retrieval_score" label="Score" width="120" />
        <el-table-column prop="chunk_text_snapshot" label="Chunk" min-width="360" />
      </el-table>
      <h3>Fusion</h3>
      <el-table :data="fusionLogs">
        <el-table-column prop="candidate_id" label="Candidate" min-width="180" />
        <el-table-column prop="candidate_type" label="Type" width="120" />
        <el-table-column prop="fusion_score" label="Fusion Score" width="140" />
        <el-table-column prop="final_rank" label="Final Rank" width="120" />
      </el-table>
      <h3>Rerank</h3>
      <el-table :data="rerankLogs">
        <el-table-column prop="candidate_id" label="Candidate" min-width="180" />
        <el-table-column prop="origin_channel" label="Channel" width="120" />
        <el-table-column prop="pre_score" label="Pre Score" width="120" />
        <el-table-column prop="rerank_score" label="Rerank Score" width="140" />
      </el-table>
      <h3>LLM</h3>
      <el-table :data="llmLogs">
        <el-table-column prop="model_name" label="Model" width="180" />
        <el-table-column prop="provider_code" label="Provider" width="160" />
        <el-table-column prop="call_status_code" label="状态" width="120" />
        <el-table-column prop="latency_ms" label="耗时" width="100" />
        <el-table-column prop="error_message" label="错误" min-width="220" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
h3 { margin-top: 20px; }
</style>
