<template>
  <aside class="w-[200px] h-full flex flex-col bg-[var(--void)]/90 backdrop-blur-[20px] border-r border-[var(--ln)] shrink-0 z-40">
    <!-- Brand Area -->
    <div class="h-[80px] flex items-center px-6 border-b border-[var(--ln)]">
      <div class="relative w-[36px] h-[36px] mr-4 flex-shrink-0">
        <div class="absolute inset-0 rounded-[8px] border border-[var(--cyan)]/40 shadow-[0_0_12px_rgba(0,229,255,0.2)]"></div>
        <div class="absolute inset-1.5 rounded-[4px] bg-gradient-to-br from-[var(--cyan)] to-[var(--violet)]"></div>
      </div>
      <div class="flex flex-col">
        <span class="text-[var(--tp)] text-[15px] font-medium leading-tight">CariesGuard</span>
        <span class="text-[var(--cyan)] font-mono text-[9px] tracking-[0.25em] leading-tight mt-1">NEURAL V2</span>
      </div>
    </div>
    
    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-6 px-3 space-y-8">
      <div v-for="group in menuGroups" :key="group.name">
        <!-- Group Label -->
        <div class="flex items-center gap-2 px-3 mb-3">
          <div class="w-[4px] h-[4px] bg-[var(--cyan)] rotate-45"></div>
          <span class="font-mono text-[10px] text-[var(--td)] tracking-[0.2em] uppercase font-medium">{{ group.name }}</span>
          <div class="flex-1 h-[1px] bg-gradient-to-r from-[var(--cyan)]/20 to-transparent ml-2"></div>
        </div>
        
        <!-- Items -->
        <div class="space-y-1">
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-3 px-3 py-[10px] rounded-[6px] text-[13px] text-[var(--ts)] transition-all relative overflow-hidden group hover:text-[var(--tp)]"
            :class="{'bg-[var(--cyan)]/10 text-[var(--tp)] font-medium': isPathActive(item.path)}"
          >
            <!-- Active Indicator -->
            <div class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-[18px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)] transition-all duration-300" :class="isPathActive(item.path) ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-full'"></div>
            
            <!-- Icon -->
            <i class="w-5 h-5 flex items-center justify-center transition-colors z-10" :class="isPathActive(item.path) ? 'text-[var(--cyan)]' : 'group-hover:text-[var(--cyan-soft)]'">
              <component :is="item.icon" v-if="item.icon" class="w-4 h-4" :stroke-width="isPathActive(item.path) ? 2.5 : 1.5" />
            </i>
            
            <span class="flex-1 z-10 tracking-wide">{{ item.name }}</span>
            
            <!-- Badge -->
            <span v-if="item.badge" class="z-10 bg-[var(--amber)]/20 text-[var(--amber)] border border-[var(--amber)]/50 font-mono text-[10px] px-2 py-0.5 rounded-[4px] shadow-[0_0_8px_rgba(255,181,71,0.2)] font-medium flex items-center justify-center min-w-[20px]">
              {{ item.badge }}
            </span>
          </router-link>
        </div>
      </div>
    </nav>
    
    <!-- Bottom User Info (Optional but good for layout) -->
    <div class="p-4 border-t border-[var(--ln)]">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-full bg-[var(--surf)] border border-[var(--td)] flex items-center justify-center overflow-hidden">
          <svg class="w-4 h-4 text-[var(--ts)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
        </div>
        <div class="flex flex-col">
          <span class="text-[12px] text-[var(--tp)] font-medium">管理员</span>
          <span class="font-mono text-[9px] text-[var(--td)] tracking-wider">ID: 1001</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import { Activity, Book, CheckSquare, List, MessageSquare, Scan } from 'lucide-vue-next';

const route = useRoute();

const isPathActive = (path: string) => {
  if (path === '/analysis' && route.path.startsWith('/analysis')) return true;
  if (path === '/rag' && route.path.startsWith('/rag')) return true;
  if (path === '/knowledge' && route.path.startsWith('/knowledge')) return true;
  return route.path === path;
};

// Mapped statically based on the spec
const menuGroups = [
  {
    name: 'AI CORE',
    items: [
      { name: '影像扫描', path: '/scan', icon: Scan },
      { name: '分析队列', path: '/analysis', icon: List },
      { name: '医生复核', path: '/review', icon: CheckSquare, badge: '3' },
      { name: '智能解释', path: '/rag', icon: MessageSquare },
    ]
  },
  {
    name: 'INTEL',
    items: [
      { name: 'AI 看板', path: '/dashboard/ai', icon: Activity },
      { name: 'RAG 轨迹', path: '/rag-trace', icon: Activity },
      { name: '知识库', path: '/knowledge', icon: Book },
    ]
  }
];
</script>
