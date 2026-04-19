<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">AI CORE / <span class="text-[var(--cyan)]">PIPELINE</span></span>
        <h2 class="text-[20px] font-medium text-[var(--tp)] m-0">分析队列</h2>
      </div>
    </div>
    
    <!-- Filter & Search Bar -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <!-- Search -->
      <div class="relative w-[320px]">
        <input 
          type="text" 
          placeholder="Search by Task ID or Patient..." 
          class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[32px] pl-9 pr-12 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] focus:shadow-[0_0_8px_rgba(0,229,255,0.2)] transition-all font-mono placeholder:text-[var(--td)]"
        />
        <svg class="w-4 h-4 text-[var(--ts)] absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
        <div class="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1 font-mono text-[8px] text-[var(--td)] border border-[var(--ln)] px-1 rounded-xs bg-[var(--void)]">
          <span>⌘</span><span>K</span>
        </div>
      </div>
      
      <!-- Tabs -->
      <div class="flex gap-2 p-1 border border-[var(--ln)] rounded-[4px] bg-[rgba(3,8,18,0.5)]">
        <button class="px-3 py-1 font-mono text-[9px] uppercase rounded-xs transition-colors flex items-center gap-1.5 text-[var(--td)] hover:text-[var(--tp)]">
          ALL <span class="text-[8px] opacity-60">2847</span>
        </button>
        <button class="px-3 py-1 font-mono text-[9px] uppercase rounded-xs transition-colors flex items-center gap-1.5 bg-[var(--cyan)]/20 text-[var(--cyan)] shadow-[0_0_8px_rgba(0,229,255,0.2)]">
          DONE <span class="text-[8px] opacity-80">2610</span>
        </button>
        <button class="px-3 py-1 font-mono text-[9px] uppercase rounded-xs transition-colors flex items-center gap-1.5 text-[var(--td)] hover:text-[var(--tp)]">
          RUNNING <span class="text-[8px] opacity-60">12</span>
        </button>
        <button class="px-3 py-1 font-mono text-[9px] uppercase rounded-xs transition-colors flex items-center gap-1.5 text-[var(--td)] hover:text-[var(--tp)]">
          REVIEW <span class="text-[8px] opacity-60">205</span>
        </button>
        <button class="px-3 py-1 font-mono text-[9px] uppercase rounded-xs transition-colors flex items-center gap-1.5 text-[var(--td)] hover:text-[var(--tp)]">
          FAILED <span class="text-[8px] opacity-60">20</span>
        </button>
      </div>
    </div>
    
    <!-- Data Table -->
    <div class="flex-1 min-h-0 glass-panel rounded-md flex flex-col overflow-hidden">
      <table class="w-full text-left border-collapse flex-1 relative">
        <thead class="bg-[rgba(3,8,18,0.7)] sticky top-0 z-10 shadow-sm border-b border-[var(--ln)]">
          <tr>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">TASK ID</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">PATIENT</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">GRADE</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">UNCERTAINTY</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">STATUS</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">CREATED</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal">DURATION</th>
            <th class="font-mono text-[8px] text-[var(--ts)] uppercase tracking-[0.12em] py-3 px-4 font-normal text-right">ACTIONS</th>
          </tr>
        </thead>
        <tbody class="overflow-y-auto block w-full absolute top-[36px] bottom-0" style="display: table-row-group;">
          <tr v-if="loading">
            <td colspan="8" class="text-center py-8 text-[var(--td)]">Loading neural pipeline data...</td>
          </tr>
          <tr v-else-if="tasks.length === 0">
            <td colspan="8" class="text-center py-8 text-[var(--td)]">No tasks found.</td>
          </tr>
          <tr v-for="(task, i) in tasks" :key="task.taskId" class="border-b border-[var(--ln)] hover:bg-[rgba(0,229,255,0.02)] transition-colors relative">
            <!-- Left status border -->
            <td class="p-0 w-0 relative">
              <div class="absolute left-0 top-0 bottom-0 w-[2px]" :class="task.taskStatusCode === 'SUCCESS' ? 'bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]' : task.taskStatusCode === 'RUNNING' ? 'bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]' : task.taskStatusCode === 'FAILED' ? 'bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]' : 'bg-[var(--td)]'"></div>
            </td>
            
            <td class="py-3 px-4">
              <router-link :to="`/analysis/${task.taskId}`" class="font-mono text-[10px] text-[var(--cyan)] val-cyan hover:underline">{{ task.taskNo || 'T-' + task.taskId }}</router-link>
            </td>
            <td class="py-3 px-4">
              <div class="flex flex-col">
                <span class="text-[12px] text-[var(--tp)]">Case-{{ task.caseId }}</span>
                <span class="font-mono text-[8px] text-[var(--td)]">{{ task.modelVersion || 'Unknown' }}</span>
              </div>
            </td>
            <td class="py-3 px-4">
              <GradeBadge :grade="'G3'" />
            </td>
            <td class="py-3 px-4 w-[140px]">
              <div class="flex items-center gap-2">
                <span class="font-mono text-[10px] text-[var(--ts)]">0.50</span>
                <div class="flex-1 h-[4px] bg-[var(--void)] border border-[var(--ln)] rounded-full overflow-hidden">
                  <div class="h-full bg-[var(--cyan)]" :style="{ width: `50%` }"></div>
                </div>
              </div>
            </td>
            <td class="py-3 px-4">
              <StatusChip :status="task.taskStatusCode" />
            </td>
            <td class="py-3 px-4 font-mono text-[10px] text-[var(--td)]">{{ new Date(task.createdAt).toLocaleTimeString() }}</td>
            <td class="py-3 px-4 font-mono text-[10px] text-[var(--ts)]">{{ task.inferenceMillis ? (task.inferenceMillis / 1000).toFixed(1) + 's' : '—' }}</td>
            <td class="py-3 px-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button class="w-[20px] h-[20px] flex items-center justify-center rounded-xs hover:bg-[var(--cyan)]/10 text-[var(--td)] hover:text-[var(--cyan)] transition-colors">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- Pagination -->
    <div class="shrink-0 h-[40px] flex items-center justify-between px-2">
      <span class="font-mono text-[9px] text-[var(--td)] tracking-widest uppercase">SHOWING {{ (pageNo - 1) * 10 + 1 }}-{{ Math.min(pageNo * 10, total) }} OF {{ total }} · SORT BY CREATED ↓</span>
      <div class="flex items-center gap-1">
        <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] hover:text-[var(--tp)]" @click="prevPage" :disabled="pageNo === 1">&lt;</button>
        <button class="w-6 h-6 flex items-center justify-center rounded-xs bg-[var(--cyan)]/20 border border-[var(--cyan)] text-[var(--cyan)] glow-cyan font-mono text-[10px]">{{ pageNo }}</button>
        <span class="text-[var(--td)] mx-1">...</span>
        <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--ts)] hover:text-[var(--tp)] font-mono text-[10px]">{{ totalPages }}</button>
        <button class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] hover:text-[var(--tp)]" @click="nextPage" :disabled="pageNo >= totalPages">&gt;</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import StatusChip from '../components/shared/StatusChip.vue';
import GradeBadge from '../components/shared/GradeBadge.vue';
import { analysisApi } from '../api/analysis';

const tasks = ref<any[]>([]);
const loading = ref(true);
const pageNo = ref(1);
const total = ref(0);
const totalPages = ref(1);

const fetchTasks = async () => {
  loading.value = true;
  try {
    const res = await analysisApi.getTasks({ pageNo: pageNo.value, pageSize: 10 });
    tasks.value = res.data.list;
    total.value = res.data.total;
    totalPages.value = Math.ceil(res.data.total / 10) || 1;
  } catch (e) {
    console.error('Failed to fetch tasks', e);
  } finally {
    loading.value = false;
  }
};

const nextPage = () => {
  if (pageNo.value < totalPages.value) {
    pageNo.value++;
    fetchTasks();
  }
};

const prevPage = () => {
  if (pageNo.value > 1) {
    pageNo.value--;
    fetchTasks();
  }
};

onMounted(() => {
  fetchTasks();
});
</script>
