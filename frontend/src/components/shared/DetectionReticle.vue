<template>
  <div 
    class="absolute pointer-events-none"
    :style="{ 
      left: `${x}%`, top: `${y}%`, 
      width: `${width}%`, height: `${height}%`,
      borderColor: `var(--${color})`
    }"
  >
    <!-- Border -->
    <div class="absolute inset-0 border border-[var(--${color})] opacity-80" :style="{ borderColor: `var(--${color})` }"></div>
    
    <!-- Corners -->
    <div class="reticle-corner reticle-tl" :style="{ borderColor: `var(--${color})` }"></div>
    <div class="reticle-corner reticle-tr" :style="{ borderColor: `var(--${color})` }"></div>
    <div class="reticle-corner reticle-bl" :style="{ borderColor: `var(--${color})` }"></div>
    <div class="reticle-corner reticle-br" :style="{ borderColor: `var(--${color})` }"></div>
    
    <!-- Label -->
    <div class="absolute -top-4 left-1/2 -translate-x-1/2 px-1 py-0.5 rounded-xs flex items-center justify-center whitespace-nowrap"
         :style="{ backgroundColor: `var(--${color})` }">
      <span class="font-mono text-[7.5px] text-[var(--void)] font-bold tracking-wider">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  color: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
}>();
</script>
