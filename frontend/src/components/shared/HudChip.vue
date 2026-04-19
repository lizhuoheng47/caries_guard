<template>
  <div class="inline-flex items-center gap-1.5 px-2 py-1 rounded-sm border" style="background: rgba(3,8,18,0.75); backdrop-filter: blur(8px);" :style="{ borderColor: `rgba(var(--${color}-rgb), 0.25)` }">
    <div class="w-1 h-1 rounded-full animate-pulse-opacity" :class="[dotClass, shadowClass]"></div>
    <span class="font-mono text-[7.5px] uppercase tracking-[0.08em]" :class="textClass">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  label: string;
  color: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
}>();

const textClass = computed(() => `text-[var(--${props.color})]`);
const dotClass = computed(() => `bg-[var(--${props.color})]`);
const shadowClass = computed(() => `shadow-[0_0_8px_var(--${props.color})]`);
</script>
