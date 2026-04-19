<template>
  <div class="glass-panel rounded-md flex flex-col h-full overflow-hidden">
    <!-- Header -->
    <div class="h-[32px] shrink-0 bg-[rgba(3,8,18,0.5)] border-b border-[var(--ln)] flex items-center justify-between px-2 relative">
      <div class="flex items-center gap-2">
        <div class="w-[3px] h-[11px]" :style="{ backgroundColor: `var(--${color})`, boxShadow: `0 0 8px var(--${color})` }"></div>
        <h3 class="text-[13px] font-medium text-[var(--tp)] m-0">{{ title }}</h3>
      </div>

      <slot name="meta"></slot>
    </div>
    
    <!-- Body -->
    <div class="flex-1 p-[12px] min-h-0 relative overflow-y-auto">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  color: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
}>();
</script>
