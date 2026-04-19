<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { kbApi } from '../../api/kb'

const docs = ref<any[]>([])
const selectedDocId = ref<string>('')
const detail = ref<any | null>(null)

const loadDocs = async () => {
  docs.value = await kbApi.documents()
  if (!selectedDocId.value && docs.value.length > 0) {
    selectedDocId.value = String(docs.value[0].id)
    await loadDetail()
  }
}

const loadDetail = async () => {
  if (!selectedDocId.value) return
  detail.value = await kbApi.documentDetail(selectedDocId.value)
}

const rollback = async (versionNo: string) => {
  if (!selectedDocId.value) return
  await kbApi.rollback(selectedDocId.value, { versionNo, comment: 'Rollback from web console' })
  ElMessage.success(`已回滚到 ${versionNo}`)
  await loadDetail()
  await loadDocs()
}

onMounted(loadDocs)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <div class="toolbar">
        <h2>发布 / 回滚记录</h2>
        <el-select v-model="selectedDocId" placeholder="选择文档" @change="loadDetail">
          <el-option v-for="doc in docs" :key="doc.id" :label="`${doc.doc_title} (${doc.doc_no})`" :value="String(doc.id)" />
        </el-select>
      </div>
      <el-empty v-if="!detail" description="暂无文档" />
      <template v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文档">{{ detail.doc_title }}</el-descriptions-item>
          <el-descriptions-item label="当前发布版本">{{ detail.published_version_no || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h3>发布记录</h3>
        <el-table :data="detail.publishRecords || []">
          <el-table-column prop="published_at" label="发布时间" width="180" />
          <el-table-column prop="action_code" label="动作" width="120" />
          <el-table-column prop="version_no" label="目标版本" width="140" />
          <el-table-column prop="previous_version_no" label="上一个版本" width="140" />
          <el-table-column prop="comment_text" label="备注" min-width="220" />
        </el-table>
        <h3>可回滚版本</h3>
        <el-table :data="detail.versions || []">
          <el-table-column prop="version_no" label="版本" width="140" />
          <el-table-column prop="review_status_code" label="审核状态" width="140" />
          <el-table-column prop="publish_status_code" label="发布状态" width="140" />
          <el-table-column prop="created_at" label="创建时间" width="180" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="rollback(row.version_no)">回滚到此版本</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 24px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; }
h3 { margin-top: 20px; }
</style>
