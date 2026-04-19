<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-3">
        <div class="flex flex-col">
          <span class="font-mono text-[11px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">情报核心 / <span class="text-[var(--violet)]">知识管理</span></span>
          <div class="flex items-center gap-2">
            <h2 class="text-[22px] font-medium text-[var(--tp)] m-0">知识库管理</h2>
            <div class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[2px] border bg-[var(--violet)]/10 border-[var(--violet)]/30">
              <div class="w-1 h-1 rounded-full bg-[var(--violet)] shadow-[0_0_8px_var(--violet)] animate-pulse-opacity"></div>
              <span class="font-mono text-[10px] text-[var(--violet)] uppercase tracking-[0.1em]">RAG 治理引擎</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="flex gap-2">
        <NeuralButton variant="ghost">重新构建索引</NeuralButton>
        <NeuralButton variant="primary">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg></template>
          上传文档
        </NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-4 gap-3 mb-4 shrink-0">
      <KpiCard label="总文档数" :value="store.stats ? String(store.stats.docs) : '—'" color="violet" />
      <KpiCard label="知识分片总数" :value="store.stats ? String(store.stats.chunks) : '—'" color="cyan" :trend="12.5" />
      <KpiCard label="最后索引时间" :value="store.stats ? formatDateOnly(store.stats.lastIndexed) : '—'" color="emerald" />
      <KpiCard label="知识库版本" :value="store.stats?.version ?? '—'" color="amber" />
    </div>
    
    <!-- Data Table -->
    <div class="flex-1 glass-panel rounded-md overflow-hidden flex flex-col min-h-0">
      <!-- Upload Area -->
      <div class="border-b border-dashed border-[var(--violet)]/20 bg-[var(--violet)]/5 p-6 flex flex-col items-center justify-center shrink-0 cursor-pointer hover:bg-[var(--violet)]/10 transition-colors">
        <svg class="w-6 h-6 text-[var(--violet)] opacity-60 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" /></svg>
        <span class="text-[13px] text-[var(--tp)] mb-1">拖拽文档至此处或点击上传</span>
        <span class="text-[11px] text-[var(--td)]">支持格式: PDF, DOCX, TXT</span>
      </div>
      
      <div class="overflow-auto flex-1 relative">
        <table class="w-full text-left border-collapse min-w-[800px]">
          <thead class="sticky top-0 bg-[rgba(3,8,18,0.85)] backdrop-blur z-10 border-b border-[var(--ln)]">
            <tr>
              <th class="py-2.5 pl-4 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">文档名称</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">类型</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">状态</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">分片数</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">实体数</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-center">版本</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">更新时间</th>
              <th class="py-2.5 pr-4 pl-2 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in store.documents.items" :key="doc.id" class="border-b border-[var(--ln)]/50 hover:bg-[var(--violet)]/5 transition-colors group relative">
              <!-- Left accent bar -->
              <td class="py-3 pl-4 px-3 relative">
                <div class="absolute left-0 top-0 bottom-0 w-[2px]" :class="getAccentColor(doc.publishStatus)"></div>
                <div class="flex flex-col">
                  <span class="text-[12px] text-[var(--tp)]">{{ doc.title }}</span>
                  <span class="font-mono text-[10px] text-[var(--td)]">{{ doc.no }}</span>
                </div>
              </td>
              <td class="py-3 px-3">
                <span class="font-mono text-[10px] px-1.5 py-0.5 rounded-[2px] border"
                      :class="doc.type === 'MANUAL' ? 'bg-[var(--violet)]/20 border-[var(--violet)]/40 text-[var(--violet)]' : doc.type === 'GUIDELINE' ? 'bg-[var(--violet)]/15 border-[var(--violet)]/30 text-[var(--violet)]' : 'bg-[var(--cyan)]/10 border-[var(--cyan)]/30 text-[var(--cyan)]'">
                  {{ typeMap[doc.type] || doc.type }}
                </span>
              </td>
              <td class="py-3 px-3">
                <div class="flex items-center gap-1.5">
                  <div class="w-1.5 h-1.5 rounded-full" :class="getDotColor(doc.publishStatus)"></div>
                  <span class="font-mono text-[10px]" :class="getStatusColor(doc.publishStatus)">{{ statusMap[doc.publishStatus] || doc.publishStatus }}</span>
                </div>
              </td>
              <td class="py-3 px-3 text-right font-mono text-[11px] text-[var(--tp)] tabular-nums">
                {{ doc.chunks || '—' }}
              </td>
              <td class="py-3 px-3 text-right font-mono text-[11px] text-[var(--tp)] tabular-nums">
                {{ doc.entities || '—' }}
              </td>
              <td class="py-3 px-3 text-center font-mono text-[10px] text-[var(--ts)]">
                {{ doc.version || '—' }}
              </td>
              <td class="py-3 px-3 font-mono text-[10px] text-[var(--td)]">
                {{ formatTime(doc.updatedAt) }}
              </td>
              <td class="py-3 pr-4 pl-2 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button class="text-[10px] font-mono text-[var(--violet)] hover:underline transition-colors">查看</button>
                  <button class="text-[10px] font-mono text-[var(--td)] hover:text-[var(--tp)] transition-colors">重引</button>
                  <button class="text-[10px] font-mono text-[var(--magenta)] hover:underline transition-colors">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="store.loading && store.documents.items.length === 0">
              <td colspan="8" class="py-16 text-center">
                <div class="flex flex-col items-center gap-3">
                  <div class="w-10 h-10 rounded-full border border-[var(--violet)]/30 border-t-[var(--violet)] animate-spin shadow-[0_0_12px_rgba(139,92,246,0.2)]"></div>
                  <span class="font-mono text-[11px] text-[var(--violet)] tracking-[0.15em] animate-pulse-opacity">正在加载知识库...</span>
                </div>
              </td>
            </tr>
            <tr v-if="!store.loading && store.documents.items.length === 0">
              <td colspan="8" class="py-16 text-center">
                <div class="flex flex-col items-center gap-2">
                  <svg class="w-8 h-8 text-[var(--td)] opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
                  <span class="text-[13px] text-[var(--td)]">知识库中暂无文档</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination -->
      <div class="h-12 border-t border-[var(--ln)] bg-[rgba(3,8,18,0.5)] flex items-center justify-between px-4 shrink-0">
        <div class="font-mono text-[11px] text-[var(--td)] uppercase tracking-[0.1em]">
          显示 {{ store.documents.items.length ? 1 : 0 }}-{{ store.documents.items.length }} 条，共 {{ store.documents.total }} 条
        </div>

        <div class="flex items-center gap-1">
          <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] hover:text-[var(--tp)] transition-colors">
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
          </button>
          <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--violet)] bg-[var(--violet)]/10 text-[var(--violet)] font-mono text-[10px] shadow-[0_0_8px_rgba(139,92,246,0.2)]">1</button>
          <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] hover:text-[var(--tp)] transition-colors">
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useKnowledgeStore } from '../stores/knowledge';
import NeuralButton from '../components/shared/NeuralButton.vue';
import KpiCard from '../components/shared/KpiCard.vue';

const store = useKnowledgeStore();

const statusMap: Record<string, string> = {
  'PUBLISHED': '已发布',
  'INDEXED': '已索引',
  'INDEXING': '索引中',
  'PENDING': '待处理',
  'UNPUBLISHED': '未发布',
  'ERROR': '错误'
};

const typeMap: Record<string, string> = {
  'MANUAL': '操作手册',
  'GUIDELINE': '临床指南',
  'RESEARCH': '科研文献'
};

onMounted(() => {

  store.fetchOverview();
  store.fetchDocuments({});
});

const getStatusColor = (status: string) => {
  switch (status) {
    case 'PUBLISHED': case 'INDEXED': return 'text-[var(--emerald)]';
    case 'INDEXING': return 'text-[var(--cyan)]';
    case 'UNPUBLISHED': case 'PENDING': return 'text-[var(--amber)]';
    case 'ERROR': return 'text-[var(--magenta)]';
    default: return 'text-[var(--td)]';
  }
};

const getDotColor = (status: string) => {
  switch (status) {
    case 'PUBLISHED': case 'INDEXED': return 'bg-[var(--emerald)] shadow-[0_0_4px_var(--emerald)]';
    case 'INDEXING': return 'bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)] animate-pulse-opacity';
    case 'UNPUBLISHED': case 'PENDING': return 'bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]';
    case 'ERROR': return 'bg-[var(--magenta)] shadow-[0_0_4px_var(--magenta)]';
    default: return 'bg-[var(--td)]';
  }
};

const getAccentColor = (status: string) => {
  switch (status) {
    case 'PUBLISHED': case 'INDEXED': return 'bg-[var(--emerald)]';
    case 'INDEXING': return 'bg-[var(--cyan)]';
    case 'ERROR': return 'bg-[var(--magenta)]';
    default: return 'bg-[var(--violet)]/50';
  }
};

const formatDateOnly = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`;
};

const formatTime = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
};
</script>
