<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-2">
        <h2 class="text-[18px] font-medium text-ice-white m-0">Analysis Pipeline</h2>
        <div class="hud-chip bg-safe-emerald/10 border-safe-emerald/30 text-safe-emerald px-1.5 py-0.5 ml-2 border-[0.5px] rounded-xs font-mono text-[8px] flex items-center gap-1">
          <div class="w-1 h-1 bg-safe-emerald rounded-full animate-pulse-opacity"></div>
          LIVE QUEUE
        </div>
      </div>
      
      <div class="flex gap-2">
        <NeuralButton variant="ghost">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg></template>
          Refresh
        </NeuralButton>
        <NeuralButton variant="primary">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg></template>
          New Task
        </NeuralButton>
      </div>
    </div>
    
    <!-- Filter & Search -->
    <div class="flex items-center justify-between mb-4 shrink-0 glass-panel p-2 rounded-md">
      <div class="flex gap-1 bg-void-black p-1 rounded-sm border border-line-subtle">
        <button 
          v-for="tab in tabs" :key="tab.value"
          @click="currentTab = tab.value"
          class="px-3 py-1 text-[11px] font-mono rounded-xs transition-colors tracking-[0.05em]"
          :class="currentTab === tab.value ? 'bg-neural-cyan/20 text-neural-cyan' : 'text-ghost-dim hover:text-steel-blue'"
        >
          {{ tab.label }} <span v-if="tab.count !== undefined" class="opacity-60 ml-1 text-[9px]">({{ tab.count }})</span>
        </button>
      </div>
      
      <div class="relative w-[240px]">
        <svg class="w-3.5 h-3.5 text-ghost-dim absolute left-2.5 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
        <input 
          type="text" 
          placeholder="Search patient, ID..." 
          class="w-full bg-void-black border border-line-subtle rounded-sm h-[28px] pl-8 pr-8 text-[11px] text-ice-white focus:outline-none focus:border-neural-cyan font-sans placeholder-ghost-dim/50"
        >
        <div class="absolute right-2 top-1/2 -translate-y-1/2 bg-deep-surface border border-line-subtle rounded-xs px-1 py-0.5 text-[8px] font-mono text-ghost-dim">⌘K</div>
      </div>
    </div>
    
    <!-- Data Table -->
    <div class="flex-1 glass-panel rounded-md overflow-hidden flex flex-col min-h-0">
      <div class="overflow-auto flex-1 relative">
        <table class="w-full text-left border-collapse min-w-[800px]">
          <thead class="sticky top-0 bg-[#030812b3] backdrop-blur z-10">
            <tr>
              <th class="py-2.5 pl-4 pr-2 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle w-[4px]"></th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Task ID</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Patient</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Grade</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Uncertainty</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Status</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Created</th>
              <th class="py-2.5 px-3 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal">Duration</th>
              <th class="py-2.5 pr-4 pl-2 text-[8px] font-mono text-steel-blue uppercase tracking-[0.12em] border-b border-line-subtle font-normal text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in store.tasks.items" :key="task.id" class="border-b border-line-subtle/50 hover:bg-neural-cyan/5 transition-colors group cursor-pointer" @click="goToDetail(task.id)">
              <td class="p-0 w-[2px]">
                <div class="h-full w-[2px] transition-all" :class="statusStripClass(task.status)"></div>
              </td>
              <td class="py-3 px-3">
                <span class="font-mono text-[10px] text-neural-cyan tracking-wider">{{ task.no }}</span>
              </td>
              <td class="py-3 px-3">
                <div class="flex flex-col">
                  <span class="text-[11px] text-ice-white">{{ task.patientName }}</span>
                  <span class="font-mono text-[9px] text-ghost-dim">{{ task.patientId }}</span>
                </div>
              </td>
              <td class="py-3 px-3">
                <GradeBadge v-if="task.grade" :grade="task.grade" />
                <span v-else class="text-ghost-dim text-[10px]">—</span>
              </td>
              <td class="py-3 px-3 w-[120px]">
                <div v-if="task.uncertainty !== undefined" class="w-full bg-void-black h-1 rounded-sm overflow-hidden border border-line-subtle">
                  <div class="h-full" :style="{ width: `${task.uncertainty * 100}%`, background: getUcColor(task.uncertainty) }"></div>
                </div>
                <span v-else class="text-ghost-dim text-[10px]">—</span>
              </td>
              <td class="py-3 px-3">
                <StatusChip :status="task.status" />
              </td>
              <td class="py-3 px-3 font-mono text-[9px] text-steel-blue">
                {{ formatTime(task.createdAt) }}
              </td>
              <td class="py-3 px-3 font-mono text-[9px] text-ghost-dim">
                {{ task.duration ? `${task.duration}ms` : '—' }}
              </td>
              <td class="py-3 pr-4 pl-2 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button class="w-5 h-5 flex items-center justify-center rounded text-ghost-dim hover:text-neural-cyan hover:bg-neural-cyan/10" title="View Details">
                    <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="store.loading && store.tasks.items.length === 0">
              <td colspan="9" class="py-12 text-center text-ghost-dim text-xs">Loading data...</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination -->
      <div class="h-10 border-t border-line-subtle bg-deep-surface flex items-center justify-between px-4 shrink-0">
        <div class="font-mono text-[8px] text-ghost-dim uppercase tracking-[0.1em]">
          SHOWING {{ store.tasks.items.length ? 1 : 0 }}-{{ store.tasks.items.length }} OF {{ store.tasks.total }} · SORT BY CREATED ↓
        </div>
        <div class="flex items-center gap-1">
          <button class="w-6 h-6 flex items-center justify-center rounded border border-line-subtle text-ghost-dim hover:text-ice-white hover:border-ice-white transition-colors">
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
          </button>
          <button class="w-6 h-6 flex items-center justify-center rounded border border-neural-cyan bg-neural-cyan/10 text-neural-cyan font-mono text-[10px]">1</button>
          <button class="w-6 h-6 flex items-center justify-center rounded border border-line-subtle text-ghost-dim hover:text-ice-white hover:border-ice-white transition-colors">
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAnalysisStore } from '../stores/analysis';
import NeuralButton from '../components/shared/NeuralButton.vue';
import StatusChip from '../components/shared/StatusChip.vue';
import GradeBadge from '../components/shared/GradeBadge.vue';

const router = useRouter();
const store = useAnalysisStore();

const currentTab = ref('ALL');
const tabs = [
  { label: 'ALL', value: 'ALL', count: 124 },
  { label: 'DONE', value: 'DONE' },
  { label: 'RUNNING', value: 'RUNNING', count: 3 },
  { label: 'REVIEW', value: 'REVIEW', count: 12 },
  { label: 'FAILED', value: 'FAILED' },
];

onMounted(() => {
  store.fetchTasks({});
});

const statusStripClass = (status: string) => {
  switch (status) {
    case 'DONE': return 'bg-safe-emerald';
    case 'RUNNING': return 'bg-neural-cyan';
    case 'REVIEW': return 'bg-alert-amber';
    case 'FAILED': return 'bg-critical-magenta';
    case 'QUEUED': return 'bg-knowledge-violet';
    default: return 'bg-transparent';
  }
};

const getUcColor = (uc: number) => {
  if (uc < 0.3) return 'var(--emerald)';
  if (uc < 0.6) return 'var(--cyan)';
  if (uc < 0.8) return 'var(--amber)';
  return 'var(--magenta)';
};

const formatTime = (iso: string) => {
  const d = new Date(iso);
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
};

const goToDetail = (id: number) => {
  router.push(`/analysis/${id}`);
};
</script>
