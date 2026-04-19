<template>
  <div class="relative glass-panel rounded-sm p-3 corner-cut-tl corner-cut-tr corner-cut-bl corner-cut-br">
    <div class="flex items-start justify-between relative px-4">
      
      <!-- Connection Lines -->
      <div class="absolute top-[8px] left-[32px] right-[32px] h-[1px] bg-[var(--td)]/30 z-0">
        <!-- Progress Line -->
        <div 
          class="absolute top-0 bottom-0 left-0 bg-[var(--cyan)] transition-all duration-500 shadow-[0_0_8px_var(--cyan)]"
          :style="{ width: `${progressPercentage}%` }"
        ></div>
      </div>

      <!-- Nodes -->
      <div 
        v-for="(node, index) in nodes" 
        :key="index"
        class="relative z-10 flex flex-col items-center gap-2"
        :style="{ flex: index === 0 || index === nodes.length - 1 ? '0 0 auto' : '1 1 0', maxWidth: index === 0 || index === nodes.length - 1 ? 'none' : '100%' }"
      >
        <!-- Node Circle -->
        <div class="w-[16px] h-[16px] rounded-full flex items-center justify-center bg-[var(--void)] relative" :class="getNodeClasses(node.status)">
          <template v-if="node.status === 'DONE'">
            <div class="absolute inset-0 bg-[var(--cyan)] rounded-full shadow-[0_0_8px_var(--cyan)]"></div>
            <svg class="w-2.5 h-2.5 text-[var(--tp)] relative z-10" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" /></svg>
          </template>
          
          <template v-else-if="node.status === 'ACTIVE'">
            <div class="absolute -inset-[3px] border-[1px] border-[var(--amber)] rounded-full animate-spin" style="animation-duration: 3s;"></div>
            <div class="w-[6px] h-[6px] rounded-full bg-[var(--amber)] shadow-[0_0_8px_var(--amber)] animate-pulse-opacity"></div>
          </template>
          
          <template v-else>
            <div class="w-[4px] h-[4px] rounded-full bg-[var(--td)] opacity-50"></div>
          </template>
        </div>
        
        <!-- Text Content -->
        <div class="flex flex-col items-center gap-0.5">
          <span class="text-[11px]" :class="node.status === 'FUTURE' ? 'text-[var(--td)]' : 'text-[var(--ts)]'">{{ node.label }}</span>
          <span class="font-mono text-[8px] uppercase tracking-wider" :class="getTimeClasses(node.status)">
            {{ node.status === 'ACTIVE' ? 'LIVE' : (node.time || '—') }}
          </span>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type NodeStatus = 'DONE' | 'ACTIVE' | 'FUTURE';

interface TimelineNode {
  label: string;
  status: NodeStatus;
  time?: string;
}

const props = defineProps<{
  nodes: TimelineNode[];
}>();

const progressPercentage = computed(() => {
  const activeIndex = props.nodes.findIndex(n => n.status === 'ACTIVE');
  if (activeIndex === -1) {
    const allDone = props.nodes.every(n => n.status === 'DONE');
    return allDone ? 100 : 0;
  }
  return (activeIndex / (props.nodes.length - 1)) * 100;
});

const getNodeClasses = (status: NodeStatus) => {
  if (status === 'DONE') return 'border-[0px]';
  if (status === 'ACTIVE') return 'border-[1px] border-[var(--amber)]';
  return 'border-[1px] border-[var(--td)]/50';
};

const getTimeClasses = (status: NodeStatus) => {
  if (status === 'ACTIVE') return 'text-[var(--amber)] val-amber animate-pulse-opacity';
  if (status === 'FUTURE') return 'text-[var(--td)]/50';
  return 'text-[var(--td)]';
};
</script>
