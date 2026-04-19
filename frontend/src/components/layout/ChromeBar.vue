<template>
  <div class="h-[28px] w-full bg-[#030812f2] flex items-center justify-between px-3 border-b border-[#ffffff0a] select-none z-50">
    <!-- Mac-like dots -->
    <div class="flex items-center gap-1.5 w-1/3">
      <div class="w-[7px] h-[7px] rounded-full bg-[#4A5F82]/50"></div>
      <div class="w-[7px] h-[7px] rounded-full bg-[#4A5F82]/50"></div>
      <div class="w-[7px] h-[7px] rounded-full bg-safe-emerald shadow-[0_0_6px_var(--emerald)]"></div>
    </div>
    
    <!-- URL Area -->
    <div class="flex-1 flex justify-center">
      <div class="flex items-center gap-1.5 px-3 py-0.5 bg-[#0A1428] rounded border border-[#ffffff0a]">
        <svg class="w-2.5 h-2.5 text-ghost-dim" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
        <span class="font-mono text-[9px] text-neural-cyan tracking-[0.08em]">console.cariesguard.ai</span>
      </div>
    </div>
    
    <!-- Status -->
    <div class="flex items-center justify-end gap-2 w-1/3">
      <div class="w-1.5 h-1.5 rounded-full bg-safe-emerald shadow-[0_0_6px_var(--emerald)] animate-pulse-opacity"></div>
      <span class="font-mono text-[9px] text-safe-emerald tracking-[0.08em]">SYSTEM ONLINE</span>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>
