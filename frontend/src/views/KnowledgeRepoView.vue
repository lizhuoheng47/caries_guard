<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-2">
        <h2 class="text-[18px] font-medium text-ice-white m-0">Knowledge Repository</h2>
        <div class="hud-chip bg-knowledge-violet/10 border-knowledge-violet/30 text-knowledge-violet px-1.5 py-0.5 ml-2 border-[0.5px] rounded-xs font-mono text-[8px] flex items-center gap-1">
          <div class="w-1 h-1 bg-knowledge-violet rounded-full animate-pulse-opacity"></div>
          RAG GOVERNANCE
        </div>
      </div>
      
      <div class="flex gap-2">
        <NeuralButton variant="ghost">Rebuild Index</NeuralButton>
        <NeuralButton variant="primary">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg></template>
          Upload Document
        </NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-4 gap-3 mb-4 shrink-0" v-if="store.stats">
      <KpiCard label="Total Documents" :value="store.stats.docs" color="violet" />
      <KpiCard label="Total Chunks" :value="store.stats.chunks" color="cyan" :trend="12.5" />
      <KpiCard label="Last Indexed" :value="formatDateOnly(store.stats.lastIndexed)" color="emerald" />
      <KpiCard label="KB Version" :value="store.stats.version" color="amber" />
    </div>
    
    <!-- Data Table -->
    <div class="flex-1 glass-panel rounded-md overflow-hidden flex flex-col min-h-0">
      <!-- Upload area (simulated) -->
      <div class="border-b border-dashed border-knowledge-violet/20 bg-knowledge-violet/5 p-4 flex flex-col items-center justify-center shrink-0">
        <svg class="w-6 h-6 text-knowledge-violet/60 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" /></svg>
        <span class="text-[11px] text-ice-white mb-1">Drag and drop documents here or click to upload</span>
        <span class="text-[9px] text-ghost-dim">Supported: PDF, DOCX, TXT</span>
      </div>
      
      <div class="overflow-auto flex-1 relative">
        <table class="w-full text-left border-collapse min-w-[800px]">
          <thead class="sticky top-0 bg-[#030812b3] backdrop-blur z-10">
            <tr>
              <th class="py-2.5 pl-4 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Document</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Type</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Status</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal text-right">Chunks</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal text-right">Entities</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal text-center">Version</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Uploaded</th>
              <th class="py-2.5 pr-4 pl-2 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in store.documents.items" :key="doc.id" class="border-b border-line-subtle/50 hover:bg-knowledge-violet/5 transition-colors group">
              <td class="py-3 pl-4 px-3">
                <div class="flex flex-col">
                  <span class="text-[11px] text-ice-white">{{ doc.title }}</span>
                  <span class="font-mono text-[9px] text-ghost-dim">{{ doc.no }}</span>
                </div>
              </td>
              <td class="py-3 px-3">
                <span class="font-mono text-[9px] px-1.5 py-0.5 rounded-sm border"
                      :class="doc.type === 'MANUAL' ? 'bg-knowledge-violet/20 border-knowledge-violet/40 text-knowledge-violet' : 'bg-cyan/10 border-cyan/30 text-cyan'">
                  {{ doc.type }}
                </span>
              </td>
              <td class="py-3 px-3">
                <div class="flex items-center gap-1.5" :class="getStatusColor(doc.publishStatus)">
                  <div class="w-1.5 h-1.5 rounded-full shadow-sm" :class="getDotColor(doc.publishStatus)"></div>
                  <span class="font-mono text-[9px]">{{ doc.publishStatus }}</span>
                </div>
              </td>
              <td class="py-3 px-3 text-right font-mono text-[10px] text-ice-white">
                {{ doc.chunks || '—' }}
              </td>
              <td class="py-3 px-3 text-right font-mono text-[10px] text-ice-white">
                {{ doc.entities || '—' }}
              </td>
              <td class="py-3 px-3 text-center font-mono text-[9px] text-steel-blue">
                {{ doc.version || '—' }}
              </td>
              <td class="py-3 px-3 font-mono text-[9px] text-ghost-dim">
                {{ formatTime(doc.updatedAt) }}
              </td>
              <td class="py-3 pr-4 pl-2 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button class="text-[9px] font-mono text-knowledge-violet hover:underline">View</button>
                  <button class="text-[9px] font-mono text-ghost-dim hover:text-ice-white">Reindex</button>
                  <button class="text-[9px] font-mono text-critical-magenta hover:underline">Delete</button>
                </div>
              </td>
            </tr>
            <tr v-if="store.loading && store.documents.items.length === 0">
              <td colspan="8" class="py-12 text-center text-ghost-dim text-xs">Loading knowledge base...</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination -->
      <div class="h-10 border-t border-line-subtle bg-deep-surface flex items-center justify-between px-4 shrink-0">
        <div class="font-mono text-[8px] text-ghost-dim uppercase tracking-[0.1em]">
          SHOWING {{ store.documents.items.length ? 1 : 0 }}-{{ store.documents.items.length }} OF {{ store.documents.total }}
        </div>
        <div class="flex items-center gap-1">
          <button class="w-6 h-6 flex items-center justify-center rounded border border-line-subtle text-ghost-dim hover:text-ice-white hover:border-ice-white transition-colors">
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
          </button>
          <button class="w-6 h-6 flex items-center justify-center rounded border border-knowledge-violet bg-knowledge-violet/10 text-knowledge-violet font-mono text-[10px]">1</button>
          <button class="w-6 h-6 flex items-center justify-center rounded border border-line-subtle text-ghost-dim hover:text-ice-white hover:border-ice-white transition-colors">
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

onMounted(() => {
  store.fetchOverview();
  store.fetchDocuments({});
});

const getStatusColor = (status: string) => {
  switch (status) {
    case 'PUBLISHED': return 'text-safe-emerald';
    case 'UNPUBLISHED': return 'text-ghost-dim';
    case 'ERROR': return 'text-critical-magenta';
    default: return 'text-alert-amber';
  }
};

const getDotColor = (status: string) => {
  switch (status) {
    case 'PUBLISHED': return 'bg-safe-emerald shadow-[0_0_4px_var(--emerald)]';
    case 'UNPUBLISHED': return 'bg-ghost-dim';
    case 'ERROR': return 'bg-critical-magenta shadow-[0_0_4px_var(--magenta)]';
    default: return 'bg-alert-amber shadow-[0_0_4px_var(--amber)]';
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
