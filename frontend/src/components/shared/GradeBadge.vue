<template>
  <div class="inline-flex items-center px-1.5 py-0.5 rounded-[2px]" :class="[bgClass]">
    <span class="font-mono text-[10px] font-medium" :class="textClass">{{ grade }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  grade: 'G0' | 'G1' | 'G2' | 'G3' | 'G4';
}>();

const theme = computed(() => {
  switch (props.grade) {
    case 'G0': return 'emerald';
    case 'G1': return 'cyan';
    case 'G2': return 'amber';
    case 'G3': return 'magenta';
    case 'G4': return 'violet';
    default: return 'cyan';
  }
});

const bgClass = computed(() => `bg-[var(--${theme.value})]/15`);
const textClass = computed(() => `text-[var(--${theme.value})]`);
</script>
