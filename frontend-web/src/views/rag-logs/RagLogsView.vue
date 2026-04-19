<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ragApi } from '../../api/rag'

const router = useRouter()
const requests = ref<any[]>([])
const detail = ref<any>(null)

const load = async () => {
  requests.value = await ragApi.requests()
}

const openDetail = async (requestNo: string) => {
  detail.value = await ragApi.requestDetail(requestNo)
}

const openRetrieval = (requestNo: string) => {
  router.push(`/rag/retrieval/${requestNo}`)
}

const openGraph = (requestNo: string) => {
  router.push(`/rag/graph/${requestNo}`)
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>RAG 日志审计</h2>
      <el-table :data="requests">
        <el-table-column prop="request_no" label="请求号" />
        <el-table-column prop="request_type_code" label="类型" />
        <el-table-column prop="user_query" label="问题" min-width="280" />
        <el-table-column prop="latency_ms" label="耗时" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.request_no)">详情</el-button>
            <el-button link type="primary" @click="openRetrieval(row.request_no)">检索明细</el-button>
            <el-button link type="primary" @click="openGraph(row.request_no)">图谱证据</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="detail" class="glass-card panel">
      <h3>{{ detail.request_no }}</h3>
      <pre>{{ detail }}</pre>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
pre { white-space: pre-wrap; }
</style>
