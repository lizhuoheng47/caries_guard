<template>
  <div 
    class="absolute border pointer-events-none"
    :style="{
      left: `${x}%`,
      top: `${y}%`,
      width: `${width}%`,
      height: `${height}%`,
      borderColor: `var(--${color})`,
      borderStyle: isDoctor ? 'dashed' : 'solid',
      borderWidth: isDoctor ? '1.5px' : '1px',
      opacity: isDoctor ? 1 : 0.8,
      zIndex: isDoctor ? 20 : 10
    }"
  >
    <!-- L-shape Corners -->
    <div class="reticle-corner reticle-tl"></div>
    <div class="reticle-corner reticle-tr"></div>
    <div class="reticle-corner reticle-bl"></div>
    <div class="reticle-corner reticle-br"></div>
    
    <!-- Label -->
    <div 
      class="absolute top-0 left-0 -translate-y-full px-1 py-0.5 text-[7.5px] font-mono whitespace-nowrap bg-opacity-90"
      :style="{ backgroundColor: `var(--${color})`, color: 'var(--void)' }"
    >
      {{ label }}
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  color?: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
  isDoctor?: boolean;
}>(), {
  color: 'amber'
});
</script>
