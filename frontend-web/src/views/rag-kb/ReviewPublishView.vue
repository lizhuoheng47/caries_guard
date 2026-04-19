<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ragApi } from '../../api/rag'

const rows = ref<any[]>([])
const load = async () => {
  rows.value = await ragApi.documents()
}
const approve = async (row: any) => {
  await ragApi.approve(row.id, { versionNo: row.current_version_no, comment: 'Approved from web' })
  ElMessage.success('审核通过')
  await load()
}
const publish = async (row: any) => {
  await ragApi.publish(row.id, { versionNo: row.current_version_no, comment: 'Publish from web' })
  ElMessage.success('已发布')
  await load()
}
onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <h2>审核与发布</h2>
      <el-table :data="rows.filter((item) => item.review_status_code !== 'PUBLISHED')">
        <el-table-column prop="doc_title" label="文档" min-width="260" />
        <el-table-column prop="current_version_no" label="版本" />
        <el-table-column prop="review_status_code" label="审核状态" />
        <el-table-column prop="publish_status_code" label="发布状态" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="approve(row)">通过</el-button>
            <el-button size="small" type="primary" @click="publish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
</style>
