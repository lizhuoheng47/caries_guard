<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ragApi } from '../../api/rag'

const form = reactive({
  scene: 'DOCTOR_QA',
  kbCode: 'caries-default',
  question: '',
  caseContext: '{}',
})
const answer = ref<any>(null)

const submit = async () => {
  answer.value = await ragApi.ask({
    scene: form.scene,
    kbCode: form.kbCode,
    question: form.question,
    caseContext: JSON.parse(form.caseContext || '{}'),
  })
}
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>三路检索调试</h2>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :md="8" :span="24">
            <el-form-item label="场景">
              <el-select v-model="form.scene"><el-option label="Doctor QA" value="DOCTOR_QA" /><el-option label="Patient Explain" value="PATIENT_EXPLAIN" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :md="16" :span="24"><el-form-item label="KB Code"><el-input v-model="form.kbCode" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="问题"><el-input v-model="form.question" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="病例上下文 JSON"><el-input v-model="form.caseContext" type="textarea" :rows="6" /></el-form-item>
        <el-button type="primary" @click="submit">检索并生成</el-button>
      </el-form>
    </div>
    <div v-if="answer" class="glass-card panel">
      <h3>答案</h3>
      <p>{{ answer.answer }}</p>
      <el-tag v-for="flag in answer.safetyFlags || []" :key="flag" class="tag">{{ flag }}</el-tag>
      <h3>引用</h3>
      <el-table :data="answer.citations || []">
        <el-table-column prop="documentCode" label="文档" />
        <el-table-column prop="score" label="分数" />
        <el-table-column prop="chunkText" label="Chunk" min-width="360" />
      </el-table>
      <h3>图谱证据</h3>
      <el-table :data="answer.graphEvidence || []">
        <el-table-column prop="cypherTemplateCode" label="Cypher 模板" />
        <el-table-column prop="score" label="分数" />
        <el-table-column prop="evidenceText" label="证据" min-width="360" />
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.tag { margin-right: 8px; }
</style>
