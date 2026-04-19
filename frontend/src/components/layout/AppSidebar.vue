<template>
  <div class="w-[180px] h-full flex flex-col shrink-0 border-r border-[var(--ln)] relative z-20" style="background: rgba(3,8,18,0.7); backdrop-filter: blur(20px);">
    <!-- Brand Area -->
    <div class="flex items-center gap-3 p-4 border-b border-[var(--ln)]">
      <div class="relative w-[26px] h-[26px] flex items-center justify-center">
        <div class="absolute inset-0 border border-[var(--cyan)] rotate-45 opacity-60 shadow-[0_0_8px_rgba(0,229,255,0.4)]"></div>
        <div class="w-[16px] h-[16px] bg-gradient-to-br from-[var(--cyan)] to-[var(--violet)] rotate-45 shadow-[0_0_8px_var(--cyan)]"></div>
      </div>
      <div class="flex flex-col">
        <span class="text-[12px] font-medium text-[var(--tp)] leading-tight">CariesGuard</span>
        <span class="font-mono text-[8px] text-[var(--cyan)] tracking-[0.2em] leading-tight mt-[2px] val-cyan">NEURAL V2</span>
      </div>
    </div>

    <!-- Navigation -->
    <div class="flex-1 overflow-y-auto py-4 px-3 flex flex-col gap-6">
      
      <!-- Category 1: AI CORE -->
      <div class="flex flex-col gap-1">
        <div class="flex items-center gap-2 mb-1 px-1">
          <div class="w-[3px] h-[3px] bg-[var(--cyan)] rotate-45 shadow-[0_0_4px_var(--cyan)]"></div>
          <span class="font-mono text-[8.5px] text-[var(--td)] tracking-[0.2em] uppercase">AI CORE</span>
          <div class="flex-1 h-[1px]" style="background: linear-gradient(90deg, rgba(0,229,255,0.2), transparent);"></div>
        </div>
        
        <router-link to="/cases" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
          <span class="flex-1 text-[12px]">影像扫描</span>
        </router-link>
        
        <router-link to="/analysis" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
          <span class="flex-1 text-[12px]">分析队列</span>
        </router-link>
        
        <router-link to="/review/1" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          <span class="flex-1 text-[12px]">医生复核</span>
          <div class="w-4 h-4 rounded-full bg-[var(--amber)]/20 border border-[var(--amber)] flex items-center justify-center font-mono text-[8px] text-[var(--amber)] shadow-[0_0_8px_rgba(255,181,71,0.3)]">3</div>
        </router-link>
        
        <router-link to="/rag" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" /></svg>
          <span class="flex-1 text-[12px]">智能解释</span>
        </router-link>
      </div>

      <!-- Category 2: INTELLIGENCE -->
      <div class="flex flex-col gap-1">
        <div class="flex items-center gap-2 mb-1 px-1">
          <div class="w-[3px] h-[3px] bg-[var(--cyan)] rotate-45 shadow-[0_0_4px_var(--cyan)]"></div>
          <span class="font-mono text-[8.5px] text-[var(--td)] tracking-[0.2em] uppercase">INTEL</span>
          <div class="flex-1 h-[1px]" style="background: linear-gradient(90deg, rgba(0,229,255,0.2), transparent);"></div>
        </div>
        
        <router-link to="/dashboard/ai" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" /></svg>
          <span class="flex-1 text-[12px]">AI 看板</span>
        </router-link>
        
        <router-link to="/knowledge" class="nav-item" active-class="nav-item-active">
          <svg class="w-4 h-4 icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.3" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
          <span class="flex-1 text-[12px]">知识库</span>
        </router-link>
      </div>

    </div>
  </div>
</template>

<style scoped>
.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  color: var(--ts);
  border-radius: 6px;
  transition: all 0.2s;
  position: relative;
  text-decoration: none;
}

.nav-item:hover {
  background: rgba(0,229,255,0.05);
}

.nav-item .icon {
  color: var(--ts);
}

.nav-item-active {
  background: linear-gradient(90deg, rgba(0,229,255,0.15), transparent);
  color: var(--cyan);
}

.nav-item-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: var(--cyan);
  box-shadow: 0 0 8px var(--cyan);
  border-radius: 2px;
}

.nav-item-active .icon {
  color: var(--cyan);
  filter: drop-shadow(0 0 4px rgba(0,229,255,0.5));
}

.nav-item-active span {
  text-shadow: 0 0 8px rgba(0,229,255,0.4);
}
</style>
