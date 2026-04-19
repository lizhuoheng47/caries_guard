<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ragApi } from '../../api/rag'

const route = useRoute()
const detail = ref<any>({})
const draftText = ref('')

const load = async () => {
  detail.value = await ragApi.documentDetail(route.params.id as string)
  draftText.value = detail.value.currentVersion?.normalized_content || ''
}

const save = async () => {
  await ragApi.updateDocument(route.params.id as string, {
    contentText: draftText.value,
    changeSummary: 'Web editor update',
  })
  ElMessage.success('已保存新版本')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>{{ detail.doc_title }}</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文档编号">{{ detail.doc_no }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{ detail.current_version_no }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ detail.review_status_code }}</el-descriptions-item>
        <el-descriptions-item label="发布状态">{{ detail.publish_status_code }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="glass-card panel">
      <h3>Markdown 编辑</h3>
      <el-input v-model="draftText" type="textarea" :rows="18" />
      <div class="actions">
        <el-button type="primary" @click="save">保存新版本</el-button>
        <el-button @click="ragApi.submitReview(route.params.id as string, { versionNo: detail.current_version_no })">提交审核</el-button>
      </div>
    </div>
    <div class="glass-card panel">
      <h3>历史版本</h3>
      <el-timeline>
        <el-timeline-item v-for="version in detail.versions || []" :key="version.id" :timestamp="version.created_at">
          {{ version.version_no }} / {{ version.review_status_code }} / {{ version.publish_status_code }}
        </el-timeline-item>
      </el-timeline>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
</style>
