<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ragApi } from '../../api/rag'

const form = reactive({
  kbCode: 'caries-default',
  question: '',
  clinicalContext: '{}',
})
const answer = ref<any>(null)

const submit = async () => {
  answer.value = await ragApi.doctorQa({
    kbCode: form.kbCode,
    question: form.question,
    clinicalContext: JSON.parse(form.clinicalContext || '{}'),
  })
}
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>Doctor QA 调试</h2>
      <el-form label-position="top">
        <el-form-item label="KB Code"><el-input v-model="form.kbCode" /></el-form-item>
        <el-form-item label="问题"><el-input v-model="form.question" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="临床上下文 JSON"><el-input v-model="form.clinicalContext" type="textarea" :rows="6" /></el-form-item>
        <el-button type="primary" @click="submit">提交</el-button>
      </el-form>
    </div>
    <div v-if="answer" class="glass-card panel">
      <h3>回答</h3>
      <p>{{ answer.answer }}</p>
      <p>RequestNo: {{ answer.requestNo }}</p>
      <p>TraceId: {{ answer.traceId }}</p>
      <p>Confidence: {{ answer.confidence }}</p>
      <p>RefusalReason: {{ answer.refusalReason || '-' }}</p>
      <el-tag v-for="flag in answer.safetyFlags || []" :key="flag" class="tag">{{ flag }}</el-tag>
      <pre>{{ answer.citations }}</pre>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.tag { margin-right: 8px; }
pre { white-space: pre-wrap; }
</style>
