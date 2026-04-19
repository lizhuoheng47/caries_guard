<template>
  <div
    class="status-chip"
    :class="`chip-${color}`"
  >
    <div class="pulse-dot"></div>
    <span class="chip-text">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status: 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED';
}>();

const colorMap: Record<string, string> = {
  DONE: 'emerald',
  RUNNING: 'cyan',
  REVIEW: 'amber',
  FAILED: 'magenta',
  QUEUED: 'violet',
};

const color = computed(() => colorMap[props.status] || 'cyan');
const label = computed(() => props.status);
</script>

<style scoped>
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  border: 0.5px solid;
}

.chip-emerald { background: rgba(0,255,163,0.1); border-color: rgba(0,255,163,0.3); }
.chip-cyan { background: rgba(0,229,255,0.1); border-color: rgba(0,229,255,0.3); }
.chip-amber { background: rgba(255,181,71,0.1); border-color: rgba(255,181,71,0.3); }
.chip-magenta { background: rgba(255,61,127,0.1); border-color: rgba(255,61,127,0.3); }
.chip-violet { background: rgba(139,92,246,0.1); border-color: rgba(139,92,246,0.3); }

.pulse-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  animation: pulse-opacity 1.5s infinite;
}

.chip-emerald .pulse-dot { background: var(--emerald); box-shadow: 0 0 6px var(--emerald); }
.chip-cyan .pulse-dot { background: var(--cyan); box-shadow: 0 0 6px var(--cyan); }
.chip-amber .pulse-dot { background: var(--amber); box-shadow: 0 0 6px var(--amber); }
.chip-magenta .pulse-dot { background: var(--magenta); box-shadow: 0 0 6px var(--magenta); }
.chip-violet .pulse-dot { background: var(--violet); box-shadow: 0 0 6px var(--violet); }

.chip-text {
  font-family: var(--font-mono);
  font-size: 8px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.chip-emerald .chip-text { color: var(--emerald); }
.chip-cyan .chip-text { color: var(--cyan); }
.chip-amber .chip-text { color: var(--amber); }
.chip-magenta .chip-text { color: var(--magenta); }
.chip-violet .chip-text { color: var(--violet); }
</style>
