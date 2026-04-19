<template>
  <sup class="citation-tag">
    [{{ id }}]
  </sup>
</template>

<script setup lang="ts">
defineProps<{
  id: string | number;
}>();
</script>

<style scoped>
.citation-tag {
  vertical-align: super;
  background: rgba(139, 92, 246, 0.2);
  border: 0.5px solid rgba(139, 92, 246, 0.4);
  color: var(--violet);
  font-family: var(--font-mono);
  font-size: 7.5px;
  padding: 0 3px;
  border-radius: var(--radius-xs);
  margin-left: 2px;
  cursor: pointer;
  transition: all 0.2s;
}

.citation-tag:hover {
  background: rgba(139, 92, 246, 0.4);
  box-shadow: 0 0 6px rgba(139, 92, 246, 0.3);
}
</style>
