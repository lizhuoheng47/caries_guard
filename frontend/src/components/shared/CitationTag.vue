<template>
  <sup class="inline-flex items-center justify-center min-w-[12px] h-[12px] px-0.5 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-[2px] ml-[2px] font-mono text-[7px] text-[var(--violet)] cursor-pointer hover:bg-[var(--violet)]/40 transition-colors align-super">
    [{{ id }}]
  </sup>
</template>

<script setup lang="ts">
defineProps<{
  id: string | number;
}>();
</script>
