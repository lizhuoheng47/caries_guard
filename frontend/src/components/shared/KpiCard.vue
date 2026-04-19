<template>
  <div class="glass-panel rounded-md p-3 relative overflow-hidden corner-cut-tr">
    <div class="flex items-center gap-1.5 mb-2">
      <div 
        class="w-1.5 h-1.5 rounded-full animate-pulse-opacity"
        :style="{ backgroundColor: `var(--${color})`, boxShadow: `0 0 6px var(--${color})` }"
      ></div>
      <span class="text-[8px] font-mono text-ghost-dim uppercase tracking-[0.12em]">{{ label }}</span>
    </div>
    
    <div class="font-mono text-xl mb-1" :class="[`val-${color}`]">{{ value }}</div>
    
    <div class="flex justify-between items-end">
      <div v-if="trend !== undefined" class="font-mono text-[8px] flex items-center gap-1" :class="trend > 0 ? 'text-safe-emerald' : 'text-alert-amber'">
        <span v-if="trend > 0">▲</span>
        <span v-else-if="trend < 0">▼</span>
        <span>{{ trend > 0 ? '+' : '' }}{{ trend }}%</span>
      </div>
      <div v-else></div>
      
      <!-- Placeholder for sparkline SVG -->
      <div class="w-[42px] h-[16px] opacity-70 flex items-end">
        <svg viewBox="0 0 42 16" class="w-full h-full overflow-visible">
          <polyline 
            points="0,12 8,8 16,14 24,6 32,10 42,2" 
            fill="none" 
            :stroke="`var(--${color})`" 
            stroke-width="1.5" 
            stroke-linecap="round" 
            stroke-linejoin="round"
            :style="{ filter: `drop-shadow(0 2px 4px var(--${color}))` }"
          />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string;
  value: string | number;
  trend?: number;
  color?: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
}>(), {
  color: 'cyan'
});
</script>
