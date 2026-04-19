<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ragApi } from '../../api/rag'

const route = useRoute()
const detail = ref<any | null>(null)
const results = ref<any[]>([])

const load = async () => {
  const runNo = String(route.params.runNo || '')
  if (!runNo) return
  detail.value = await ragApi.evalRunDetail(runNo)
  results.value = await ragApi.evalRunResults(runNo)
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel" v-if="detail">
      <h2>Eval Run {{ detail.run_no }}</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="数据集">{{ detail.dataset_name }} ({{ detail.dataset_code }})</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.run_status_code }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.started_at }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.finished_at || '-' }}</el-descriptions-item>
      </el-descriptions>
      <h3>聚合指标</h3>
      <pre>{{ detail.metric_json }}</pre>
    </div>
    <div class="glass-card panel">
      <h3>单题结果</h3>
      <el-table :data="results">
        <el-table-column prop="question_no" label="题号" width="120" />
        <el-table-column prop="scene_code" label="场景" width="140" />
        <el-table-column prop="question_text" label="问题" min-width="280" />
        <el-table-column prop="citation_hit_flag" label="Citation Hit" width="110" />
        <el-table-column prop="graph_hit_flag" label="Graph Hit" width="100" />
        <el-table-column prop="refusal_hit_flag" label="Refusal Hit" width="110" />
        <el-table-column prop="hallucination_flag" label="Hallucination" width="120" />
        <el-table-column prop="latency_ms" label="Latency" width="100" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
pre { white-space: pre-wrap; }
</style>
