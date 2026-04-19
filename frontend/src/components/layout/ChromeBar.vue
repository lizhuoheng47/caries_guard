<template>
  <div class="h-[28px] w-full bg-[rgba(3,8,18,0.95)] flex items-center px-4 shrink-0 relative border-b border-[var(--ln)]">
    <!-- Traffic lights -->
    <div class="flex items-center gap-[6px]">
      <div class="w-[7px] h-[7px] rounded-full bg-[var(--td)] opacity-50"></div>
      <div class="w-[7px] h-[7px] rounded-full bg-[var(--td)] opacity-50"></div>
      <div class="w-[7px] h-[7px] rounded-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]"></div>
    </div>
    
    <!-- URL Area -->
    <div class="absolute left-1/2 -translate-x-1/2 flex items-center gap-2">
      <div class="w-[6px] h-[6px] rotate-45 bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]"></div>
      <span class="font-mono text-[11px] text-[var(--cyan)] tracking-[0.08em] val-cyan">龋齿卫士 / {{ currentRouteNameTranslated }}</span>
    </div>

    <!-- Status Area -->
    <div class="ml-auto flex items-center gap-4">
      <!-- Theme Toggle -->
      <button 
        @click="uiStore.toggleTheme" 
        class="w-5 h-5 flex items-center justify-center rounded-xs hover:bg-[var(--cyan)]/10 text-[var(--ts)] hover:text-[var(--cyan)] transition-colors"
        :title="uiStore.theme === 'dark' ? '切换至浅色模式' : '切换至深色模式'"
      >
        <svg v-if="uiStore.theme === 'dark'" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364-6.364l-.707.707M6.343 17.657l-.707.707M17.657 17.657l.707-.707M6.343 6.343l.707-.707M12 8a4 4 0 100 8 4 4 0 000-8z" /></svg>
        <svg v-else class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
      </button>

      <div class="flex items-center gap-2">
        <div class="w-1.5 h-1.5 rounded-full bg-[var(--emerald)] shadow-[0_0_6px_var(--emerald)] animate-pulse-opacity"></div>
        <span class="font-mono text-[11px] text-[var(--emerald)] tracking-[0.08em] uppercase val-emerald">神经链路稳定</span>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useUiStore } from '@/stores/ui';

const route = useRoute();
const uiStore = useUiStore();
const routeNameMap: Record<string, string> = {
  'DASHBOARD-AI': 'AI 指挥部',
  'ANALYSIS-QUEUE': '分析队列',
  'ANALYSIS-DETAIL': '诊断详情',
  'REVIEW-WORKBENCH': '复核工作台',
  'RAG-CONSOLE': '智能解释',
  'KNOWLEDGE-REPO': '知识库',
  'CASE-PORTAL': '影像扫描',
  'LOGIN': '系统登录'
};

const currentRouteNameTranslated = computed(() => {
  const name = (route.name as string || '').toUpperCase();
  return routeNameMap[name] || name || '控制面板';
});
</script>

