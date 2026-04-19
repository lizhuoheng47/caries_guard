<template>
  <div class="grade-badge" :class="`grade-${grade}`">
    {{ grade }}
  </div>
</template>

<script setup lang="ts">
defineProps<{
  grade: string; // G0, G1, G2, G3, G4
}>();
</script>

<style scoped>
.grade-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
}

.grade-G0 { background: rgba(0,255,163,0.15); color: var(--emerald); }
.grade-G1 { background: rgba(0,229,255,0.15); color: var(--cyan); }
.grade-G2 { background: rgba(255,181,71,0.15); color: var(--amber); }
.grade-G3 { background: rgba(255,61,127,0.15); color: var(--magenta); }
.grade-G4 { background: rgba(139,92,246,0.15); color: var(--violet); }
</style>
