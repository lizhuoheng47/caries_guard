<template>
  <div class="flex flex-col w-full">
    <div class="flex justify-between items-end mb-1">
      <span class="font-mono text-[8px] text-[var(--amber)] uppercase tracking-[0.1em]">Uncertainty</span>
      <span class="font-mono text-[16px] text-[var(--amber)] val-amber leading-none">{{ value.toFixed(2) }}</span>
    </div>
    
    <div class="relative w-full h-[6px] bg-[rgba(0,0,0,0.3)] border-[0.5px] border-[var(--ln)] rounded-[2px] overflow-hidden mb-1">
      <div 
        class="absolute top-0 bottom-0 left-0"
        :style="{ width: `${value * 100}%`, background: 'linear-gradient(90deg, var(--cyan), var(--amber), var(--magenta))' }"
      ></div>
      <!-- Pulse dot at the end -->
      <div 
        class="absolute top-1/2 -translate-y-1/2 w-[2px] h-[4px] bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]"
        :style="{ left: `calc(${value * 100}% - 2px)` }"
      ></div>
    </div>
    
    <div class="flex justify-between relative">
      <span class="font-mono text-[8px] text-[var(--td)]">LOW</span>
      <span class="font-mono text-[8px] text-[var(--td)] absolute left-[35%] -translate-x-1/2 flex items-center flex-col">
        <div class="w-[1px] h-[3px] bg-[var(--amber)] mb-[1px]"></div>
        Θ 0.35
      </span>
      <span class="font-mono text-[8px] text-[var(--td)]">HIGH</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  value: number;
}>();
</script>
