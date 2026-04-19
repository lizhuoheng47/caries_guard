<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { kbApi } from '../../api/kb'

const overview = ref<Record<string, any>>({})
onMounted(async () => {
  overview.value = await kbApi.overview()
})
</script>

<template>
  <section class="page-grid">
    <div class="glass-card hero">
      <div>
        <small>真实知识底座</small>
        <h2>知识图库与 AI 检索总览</h2>
      </div>
      <el-button type="primary" @click="$router.push('/kb/upload')">导入新文档</el-button>
    </div>
    <div class="metrics">
      <div class="metric glass-card" v-for="item in [
        ['知识库', overview.knowledgeBaseCount],
        ['文档', overview.documentCount],
        ['Chunks', overview.chunkCount],
        ['实体', overview.entityCount],
        ['关系', overview.relationCount],
        ['问答请求', overview.requestCount],
        ['评估运行', overview.evalRunCount],
      ]" :key="item[0]">
        <span>{{ item[0] }}</span>
        <strong>{{ item[1] ?? 0 }}</strong>
      </div>
    </div>
    <el-row :gutter="20">
      <el-col :md="12" :span="24">
        <div class="glass-card panel">
          <h3>最近重建</h3>
          <pre>{{ overview.latestRebuildJob }}</pre>
        </div>
      </el-col>
      <el-col :md="12" :span="24">
        <div class="glass-card panel">
          <h3>最近评估</h3>
          <pre>{{ overview.latestEvalRun }}</pre>
        </div>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.hero, .panel { padding: 24px; }
.hero { display: flex; justify-content: space-between; align-items: center; }
.hero small { color: var(--cg-accent-2); letter-spacing: 0.16em; text-transform: uppercase; }
.hero h2 { margin: 8px 0 0; font-size: 34px; }
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.metric { padding: 20px; display: grid; gap: 8px; }
.metric span { color: var(--cg-muted); }
.metric strong { font-size: 34px; color: var(--cg-accent); }
pre { white-space: pre-wrap; }
</style>
