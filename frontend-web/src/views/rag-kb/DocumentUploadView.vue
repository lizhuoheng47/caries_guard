<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { ragApi } from '../../api/rag'

const form = reactive({
  kbCode: 'caries-default',
  kbName: 'CariesGuard Default Knowledge Base',
  kbTypeCode: 'PATIENT_GUIDE',
  docTitle: '',
  docSourceCode: 'UPLOAD',
  docVersion: 'v1.0',
  sourceUri: '',
  changeSummary: '',
})

const upload = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(form).forEach(([key, value]) => formData.append(key, value))
  const result = await ragApi.upload(formData)
  ElMessage.success(`已创建导入任务 ${result.ingestJobNo}`)
}

const importText = async () => {
  const result = await ragApi.importText({
    kbCode: form.kbCode,
    kbName: form.kbName,
    kbTypeCode: form.kbTypeCode,
    docTitle: form.docTitle,
    docSourceCode: 'MANUAL',
    sourceUri: form.sourceUri,
    docVersion: form.docVersion,
    contentText: form.changeSummary,
  })
  ElMessage.success(`已导入文档 ${result.docNo}`)
}
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>文档导入</h2>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :md="12" :span="24"><el-form-item label="KB Code"><el-input v-model="form.kbCode" /></el-form-item></el-col>
          <el-col :md="12" :span="24"><el-form-item label="KB Name"><el-input v-model="form.kbName" /></el-form-item></el-col>
          <el-col :md="12" :span="24"><el-form-item label="文档标题"><el-input v-model="form.docTitle" /></el-form-item></el-col>
          <el-col :md="12" :span="24"><el-form-item label="版本号"><el-input v-model="form.docVersion" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="来源 URI"><el-input v-model="form.sourceUri" /></el-form-item>
        <el-form-item label="文本导入内容">
          <el-input v-model="form.changeSummary" type="textarea" :rows="8" placeholder="用于快速导入结构化文本" />
        </el-form-item>
        <div class="actions">
          <el-upload :show-file-list="false" :before-upload="upload">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
          <el-button @click="importText">导入文本</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.actions { display: flex; gap: 12px; }
</style>
