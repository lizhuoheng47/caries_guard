<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ragApi } from '../../api/rag'

const router = useRouter()
const datasets = ref<any[]>([])
const runs = ref<any[]>([])
const datasetId = ref<number | null>(1)

const load = async () => {
  datasets.value = await ragApi.evalDatasets()
  runs.value = await ragApi.evalRuns()
}

const run = async () => {
  if (!datasetId.value) return
  const result = await ragApi.runEval({ datasetId: datasetId.value })
  ElMessage.success(`评估启动: ${result.runNo}`)
  await load()
}

const openResults = (runNo: string) => {
  router.push(`/rag/eval/${runNo}`)
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
      <h2>评估数据集</h2>
      <el-table :data="datasets">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="dataset_code" label="编码" width="180" />
        <el-table-column prop="dataset_name" label="名称" min-width="240" />
        <el-table-column prop="active_flag" label="启用" width="90" />
      </el-table>
    </div>
    <div class="glass-card panel">
      <el-table :data="runs">
        <el-table-column prop="run_no" label="运行编号" />
        <el-table-column prop="dataset_name" label="数据集" min-width="220" />
        <el-table-column prop="run_status_code" label="状态" />
        <el-table-column prop="started_at" label="开始时间" />
        <el-table-column prop="metric_json" label="指标" min-width="320">
          <template #default="{ row }"><pre>{{ row.metric_json }}</pre></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openResults(row.run_no)">查看结果</el-button>
          </template>
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
