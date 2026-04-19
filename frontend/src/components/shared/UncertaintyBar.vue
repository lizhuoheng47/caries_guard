<template>
  <div class="uncertainty-bar w-full">
    <div class="flex justify-between items-end mb-1">
      <span class="text-[8px] font-mono text-alert-amber uppercase">Uncertainty</span>
      <span class="text-base font-mono text-alert-amber val-amber">{{ value.toFixed(2) }}</span>
    </div>
    
    <div class="relative w-full h-[6px] bg-black/30 border-[0.5px] border-line-subtle rounded-sm overflow-hidden">
      <div 
        class="absolute top-0 left-0 h-full transition-all duration-300"
        :style="{
          width: `${value * 100}%`,
          background: 'linear-gradient(90deg, var(--cyan), var(--amber), var(--magenta))'
        }"
      >
        <div class="absolute right-0 top-1/2 -translate-y-1/2 w-[2px] h-[2px] bg-alert-amber shadow-[0_0_6px_var(--amber)]"></div>
      </div>
      
      <!-- Threshold line -->
      <div 
        class="absolute top-0 bottom-0 border-l border-dashed border-alert-amber/50"
        :style="{ left: `${threshold * 100}%` }"
      ></div>
    </div>
    
    <div class="flex justify-between mt-1 text-[8px] font-mono text-ghost-dim">
      <span>LOW</span>
      <span class="absolute" :style="{ left: `calc(${threshold * 100}% - 12px)` }">Θ {{ threshold }}</span>
      <span>HIGH</span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  value: number; // 0.0 to 1.0
  threshold?: number;
}>(), {
  threshold: 0.35
});
</script>
