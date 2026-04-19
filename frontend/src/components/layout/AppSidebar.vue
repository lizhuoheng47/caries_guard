<template>
  <aside class="w-[180px] h-full flex flex-col bg-[#030812b3] backdrop-blur-[20px] border-r-[0.5px] border-line-subtle shrink-0">
    <!-- Brand Area -->
    <div class="h-[60px] flex items-center px-4 border-b-[0.5px] border-line-subtle">
      <div class="relative w-[26px] h-[26px] mr-3">
        <div class="absolute inset-0 border border-neural-cyan rotate-45 opacity-60 shadow-[0_0_8px_var(--cyan)]"></div>
        <div class="absolute inset-1 bg-gradient-to-br from-neural-cyan to-knowledge-violet"></div>
      </div>
      <div class="flex flex-col">
        <span class="text-ice-white text-xs font-medium leading-tight">CariesGuard</span>
        <span class="text-neural-cyan font-mono text-[8px] tracking-[0.2em] leading-tight mt-0.5">NEURAL V2</span>
      </div>
    </div>
    
    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-4 px-2 space-y-6">
      <div v-for="group in menuGroups" :key="group.name">
        <!-- Group Label -->
        <div class="flex items-center gap-1.5 px-2 mb-2">
          <div class="w-[3px] h-[3px] bg-neural-cyan rotate-45"></div>
          <span class="font-mono text-[8.5px] text-ghost-dim tracking-[0.2em] uppercase">{{ group.name }}</span>
          <div class="flex-1 h-[1px] bg-gradient-to-r from-neural-cyan/20 to-transparent"></div>
        </div>
        
        <!-- Items -->
        <div class="space-y-1">
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-2 px-2.5 py-[7px] rounded-[6px] text-xs text-steel-blue transition-all group hover:bg-neural-cyan/5 relative overflow-hidden"
            active-class="active-nav-item"
          >
            <!-- Active Indicator -->
            <div class="absolute left-0 top-0 bottom-0 w-[2px] bg-neural-cyan shadow-[0_0_8px_var(--cyan)] opacity-0 transition-opacity" :class="{'opacity-100': isPathActive(item.path)}"></div>
            
            <i class="w-4 h-4 flex items-center justify-center transition-colors" :class="{'text-neural-cyan': isPathActive(item.path)}">
              <component :is="item.icon" v-if="item.icon" class="w-3.5 h-3.5" />
            </i>
            <span class="flex-1">{{ item.name }}</span>
            
            <span v-if="item.badge" class="bg-alert-amber text-void-black font-mono text-[9px] px-1.5 py-0.5 rounded-xs">
              {{ item.badge }}
            </span>
          </router-link>
        </div>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
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
      { name: '医生复核', path: '/review', icon: CheckSquare, badge: '12' },
      { name: '智能解释', path: '/rag', icon: MessageSquare },
    ]
  },
  {
    name: 'INTELLIGENCE',
    items: [
      { name: 'AI 评估看板', path: '/dashboard/ai', icon: Activity },
      { name: '知识图库', path: '/knowledge', icon: Book },
    ]
  }
];
</script>

<style scoped>
.active-nav-item {
  background: linear-gradient(90deg, rgba(0,229,255,0.1), transparent);
  color: var(--tp);
  font-weight: 500;
}
</style>
