<template>
  <div class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[2px] border" :class="[bgClass, borderClass]">
    <div class="w-1 h-1 rounded-full animate-pulse-opacity" :class="[dotClass, shadowClass]"></div>
    <span class="font-mono text-[8px] uppercase tracking-[0.1em]" :class="textClass">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status: 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED';
}>();

const label = computed(() => props.status);

const theme = computed(() => {
  switch (props.status) {
    case 'DONE': return 'emerald';
    case 'RUNNING': return 'cyan';
    case 'REVIEW': return 'amber';
    case 'FAILED': return 'magenta';
    case 'QUEUED': return 'violet';
    default: return 'cyan';
  }
});

const bgClass = computed(() => `bg-[var(--${theme.value})]/10`);
const borderClass = computed(() => `border-[var(--${theme.value})]/30`);
const textClass = computed(() => `text-[var(--${theme.value})]`);
const dotClass = computed(() => `bg-[var(--${theme.value})]`);
const shadowClass = computed(() => `shadow-[0_0_8px_var(--${theme.value})]`);
</script>
