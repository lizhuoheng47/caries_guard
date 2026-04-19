<template>
  <div class="h-[36px] w-full bg-[var(--void)] flex items-center justify-between px-4 border-b border-[var(--ln)] select-none z-50 shrink-0">
    <!-- Mac-like dots -->
    <div class="flex items-center gap-2 w-1/4">
      <div class="w-3 h-3 rounded-full border border-[var(--td)]"></div>
      <div class="w-3 h-3 rounded-full border border-[var(--td)]"></div>
      <div class="w-3 h-3 rounded-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]"></div>
    </div>
    
    <!-- URL Area / Title -->
    <div class="flex-1 flex justify-center items-center gap-2">
      <div class="w-2 h-2 rotate-45 bg-[var(--cyan)]"></div>
      <span class="font-mono text-[11px] text-[var(--cyan)] tracking-[0.25em] uppercase font-medium">CARIESGUARD.NEURAL · 控制台 · T-20260418-001</span>
    </div>
    
    <!-- Status -->
    <div class="flex items-center justify-end gap-2 w-1/4">
      <div class="w-2 h-2 rounded-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)] animate-pulse-opacity"></div>
      <span class="font-mono text-[10px] text-[var(--emerald)] tracking-[0.15em] font-medium">神经网络连接稳定</span>
    </div>
  </div>
</template>
