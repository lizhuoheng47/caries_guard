<template>
  <div class="relative glass-panel rounded-md p-3 flex flex-col justify-between corner-cut-tr">
    <!-- Top Row -->
    <div class="flex items-center gap-1.5 mb-2">
      <div class="w-[4px] h-[4px] rounded-full animate-pulse-opacity" :style="{ backgroundColor: `var(--${color})`, boxShadow: `0 0 8px var(--${color})` }"></div>
      <span class="font-mono text-[8px] text-[var(--td)] uppercase tracking-[0.1em]">{{ label }}</span>
    </div>
    
    <!-- Value & Trend Row -->
    <div class="flex items-baseline gap-2">
      <span class="font-mono text-[22px] font-medium" :class="[`val-${color}`]">{{ value }}</span>
      <span v-if="trend" class="font-mono text-[8px]" :class="trend > 0 ? 'text-[var(--emerald)]' : 'text-[var(--amber)]'">
        {{ trend > 0 ? '▲' : '▼' }} {{ Math.abs(trend) }}%
      </span>
    </div>
    
    <!-- Sparkline (Optional) -->
    <div v-if="sparkline" class="absolute bottom-2 right-2 w-[42px] h-[16px] opacity-70">
      <svg viewBox="0 0 42 16" class="w-full h-full overflow-visible">
        <polyline 
          :points="sparkline" 
          fill="none" 
          :stroke="`var(--${color})`" 
          stroke-width="1.5" 
          stroke-linecap="round"
          stroke-linejoin="round"
          :style="{ filter: `drop-shadow(0 0 4px var(--${color}))` }"
        />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  value: string;
  color: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
  trend?: number;
  sparkline?: string;
}>();
</script>
