<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { kbApi } from '../../api/kb'

const stats = ref<Record<string, any>>({})

const load = async () => {
  stats.value = await kbApi.graphStats()
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>图谱统计</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="KB Code">{{ stats.kbCode }}</el-descriptions-item>
        <el-descriptions-item label="实体数">{{ stats.entityCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="关系数">{{ stats.relationCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Embedding Provider">{{ stats.embeddingProvider }}</el-descriptions-item>
        <el-descriptions-item label="Embedding Model">{{ stats.embeddingModel }}</el-descriptions-item>
        <el-descriptions-item label="Embedding Version">{{ stats.embeddingVersion }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
</style>
