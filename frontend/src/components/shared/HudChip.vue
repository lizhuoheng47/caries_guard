<template>
  <div 
    class="hud-chip"
    :style="{
      borderColor: `color-mix(in srgb, var(--${color}) 25%, transparent)`,
      color: `var(--${color})`
    }"
  >
    <div 
      class="pulse-dot"
      :style="{
        backgroundColor: `var(--${color})`,
        boxShadow: `0 0 6px var(--${color})`
      }"
    ></div>
    <span>{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string;
  color?: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
}>(), {
  color: 'cyan'
});
</script>

<style scoped>
.hud-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(3, 8, 18, 0.75);
  backdrop-filter: blur(8px);
  padding: 2px 6px;
  border: 0.5px solid;
  border-radius: var(--radius-xs);
  font-family: var(--font-mono);
  font-size: 7.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.pulse-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  animation: pulse-opacity 1.5s infinite;
}
</style>
