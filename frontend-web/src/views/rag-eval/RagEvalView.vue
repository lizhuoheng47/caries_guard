<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ragApi } from '../../api/rag'

const runs = ref<any[]>([])
const datasetId = ref<number | null>(1)

const load = async () => {
  runs.value = await ragApi.evalRuns()
}

const run = async () => {
  if (!datasetId.value) return
  const result = await ragApi.runEval({ datasetId: datasetId.value })
  ElMessage.success(`评估启动: ${result.runNo}`)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>自动评估</h2>
      <div class="actions">
        <el-input-number v-model="datasetId" :min="1" />
        <el-button type="primary" @click="run">执行评估</el-button>
      </div>
    </div>
    <div class="glass-card panel">
      <el-table :data="runs">
        <el-table-column prop="run_no" label="运行编号" />
        <el-table-column prop="run_status_code" label="状态" />
        <el-table-column prop="started_at" label="开始时间" />
        <el-table-column prop="metric_json" label="指标" min-width="320">
          <template #default="{ row }"><pre>{{ row.metric_json }}</pre></template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.actions { display: flex; gap: 12px; align-items: center; }
pre { white-space: pre-wrap; }
</style>
