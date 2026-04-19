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
      <span class="font-mono text-[9px] text-[var(--cyan)] tracking-[0.08em] val-cyan">CARIESGUARD / {{ currentRouteName }}</span>
    </div>

    <!-- Status Area -->
    <div class="ml-auto flex items-center gap-2">
      <div class="w-1.5 h-1.5 rounded-full bg-[var(--emerald)] shadow-[0_0_6px_var(--emerald)] animate-pulse-opacity"></div>
      <span class="font-mono text-[9px] text-[var(--emerald)] tracking-[0.08em] uppercase val-emerald">NEURAL LINK STABLE</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const currentRouteName = computed(() => {
  const path = route.path.replace(/^\//, '').toUpperCase();
  return path || 'DASHBOARD';
});
</script>
