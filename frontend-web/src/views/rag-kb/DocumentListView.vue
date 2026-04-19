<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ragApi } from '../../api/rag'

const filters = reactive({ keyword: '' })
const rows = ref<any[]>([])
const load = async () => {
  rows.value = await ragApi.documents({ keyword: filters.keyword || undefined })
}
onMounted(load)
</script>

<template>
  <section class="page-grid">
    <div class="glass-card panel">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="标题 / 编号" />
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
    </div>
    <div class="glass-card panel">
      <el-table :data="rows">
        <el-table-column prop="doc_no" label="文档编号" />
        <el-table-column prop="doc_title" label="标题" min-width="280" />
        <el-table-column prop="current_version_no" label="当前版本" />
        <el-table-column prop="review_status_code" label="审核状态" />
        <el-table-column prop="publish_status_code" label="发布状态" />
        <el-table-column prop="updated_at" label="更新时间" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/kb/documents/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.panel { padding: 20px; }
</style>
