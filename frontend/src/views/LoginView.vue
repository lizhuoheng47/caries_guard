<template>
  <div class="h-screen w-screen page-bg flex flex-col overflow-hidden relative">
    <!-- Floating particles background effect -->
    <div class="absolute inset-0 z-0 pointer-events-none">
      <div class="absolute w-[3px] h-[3px] rounded-full bg-[var(--cyan)] animate-float-particle" style="left: 20%; top: 30%; animation-delay: 0s;"></div>
      <div class="absolute w-[3px] h-[3px] rounded-full bg-[var(--violet)] animate-float-particle" style="left: 70%; top: 20%; animation-delay: 1s;"></div>
      <div class="absolute w-[3px] h-[3px] rounded-full bg-[var(--emerald)] animate-float-particle" style="left: 40%; top: 70%; animation-delay: 2s;"></div>
      <div class="absolute w-[3px] h-[3px] rounded-full bg-[var(--magenta)] animate-float-particle" style="left: 80%; top: 60%; animation-delay: 0.5s;"></div>
      <div class="absolute w-[3px] h-[3px] rounded-full bg-[var(--cyan)] animate-float-particle" style="left: 10%; top: 80%; animation-delay: 1.5s;"></div>
      
      <!-- Specialized Radial Glows for Login -->
      <div class="absolute inset-0 opacity-20" style="background: radial-gradient(circle at center, var(--emerald) 0%, transparent 60%); filter: blur(40px);"></div>
    </div>
    
    <!-- Top Bar -->
    <div class="h-[28px] w-full flex items-center px-4 relative z-20 shrink-0">
      <div class="flex gap-[6px]">
        <div class="w-[7px] h-[7px] rounded-full border border-[var(--td)] opacity-50"></div>
        <div class="w-[7px] h-[7px] rounded-full border border-[var(--td)] opacity-50"></div>
        <div class="w-[7px] h-[7px] rounded-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]"></div>
      </div>
      <div class="absolute left-1/2 -translate-x-1/2 flex items-center gap-2">
        <div class="w-[6px] h-[6px] rotate-45 bg-[var(--cyan)]"></div>
        <span class="font-mono text-[9px] text-[var(--cyan)] tracking-[0.08em] uppercase val-cyan">CARIESGUARD.NEURAL / AUTH</span>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 flex max-w-[1100px] mx-auto w-full items-center z-10 relative px-12">
      <!-- Left Column: Brand Area -->
      <div class="flex-1 pr-16 flex flex-col justify-center">
        <!-- Logo -->
        <div class="flex items-center gap-4 mb-8">
          <div class="relative w-[60px] h-[60px] rounded-lg border border-[var(--cyan)]/30 flex items-center justify-center shadow-[0_0_20px_rgba(0,229,255,0.2)]">
            <div class="absolute inset-0 rounded-lg bg-gradient-to-br from-[var(--cyan)]/20 to-[var(--violet)]/10"></div>
            <div class="w-[32px] h-[32px] rounded bg-gradient-to-br from-[var(--cyan-soft)] to-[var(--violet)] shadow-[0_0_15px_var(--cyan)]"></div>
            <!-- Orbit Ring -->
            <div class="absolute -inset-2 border border-dashed border-[var(--cyan)]/40 rounded-full animate-orbit"></div>
            <!-- Particle -->
            <div class="absolute -top-3 left-1/2 w-1 h-1 rounded-full bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]"></div>
          </div>
          <div class="flex flex-col">
            <h1 class="text-[28px] text-[var(--tp)] m-0 font-medium tracking-wide leading-none">CariesGuard</h1>
            <span class="font-mono text-[11px] text-[var(--cyan)] tracking-[0.2em] mt-1 val-cyan">NEURAL CONSOLE V2</span>
          </div>
        </div>
        
        <div class="inline-flex items-center gap-3 mb-8 bg-[var(--emerald)]/10 border border-[var(--emerald)]/20 px-4 py-1.5 rounded-[2px] w-fit">
          <div class="w-2 h-2 rounded-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)] animate-pulse-opacity"></div>
          <span class="font-mono text-[10px] text-[var(--emerald)] tracking-[0.1em] uppercase val-emerald">System Online · All Checks Passed</span>
        </div>
        
        <h2 class="text-[36px] font-medium leading-[1.3] mb-6 tracking-wider bg-clip-text text-transparent bg-gradient-to-r from-[var(--tp)] via-[var(--cyan-soft)] to-[var(--violet)]">
          医疗 AI 辅助<br/>决策闭环系统
        </h2>
        
        <p class="text-[11px] text-[var(--ts)] max-w-[420px] leading-relaxed mb-16">
          登录进入全息诊断舱，访问影像 AI 分析、RAG 智能解释、医生复核闭环与知识治理核心能力。
        </p>
        
        <div class="flex items-center gap-8 border-t border-[var(--ln)] pt-6">
          <div class="flex flex-col gap-1">
            <span class="font-mono text-[20px] font-medium val-cyan tabular-nums">99.2%</span>
            <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase">Uptime</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="font-mono text-[20px] font-medium val-cyan tabular-nums">v1.0</span>
            <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase">Model</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="font-mono text-[20px] font-medium val-cyan tabular-nums">24/7</span>
            <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase">Online</span>
          </div>
        </div>
      </div>
      
      <!-- Right Column: Login Card -->
      <div class="w-[420px] relative">
        <!-- Panel shadow -->
        <div class="absolute inset-0 bg-black/40 blur-xl rounded-xl transform translate-y-4"></div>
        
        <div class="glass-panel p-10 relative z-10 rounded-md border-[var(--cyan)]/20 shadow-[0_20px_40px_rgba(0,0,0,0.4)]">
          <!-- Reticle Corners -->
          <div class="reticle-corner reticle-tl border-[var(--cyan)]/60 w-[8px] h-[8px]"></div>
          <div class="reticle-corner reticle-tr border-[var(--cyan)]/60 w-[8px] h-[8px]"></div>
          <div class="reticle-corner reticle-bl border-[var(--cyan)]/60 w-[8px] h-[8px]"></div>
          <div class="reticle-corner reticle-br border-[var(--cyan)]/60 w-[8px] h-[8px]"></div>
          
          <div class="flex items-center gap-3 mb-1">
            <div class="w-[3px] h-[16px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]"></div>
            <h3 class="text-[14px] font-medium text-[var(--tp)] m-0 tracking-wide">Secure Access</h3>
          </div>
          <div class="font-mono text-[8px] text-[var(--td)] tracking-[0.15em] mb-10 pl-[15px]">AUTHENTICATION REQUIRED</div>
          
          <form @submit.prevent="handleLogin" class="flex flex-col gap-6">
            <div class="flex flex-col gap-2">
              <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Username</label>
              <div class="relative">
                <input 
                  type="text" 
                  v-model="username"
                  class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[4px] h-[44px] px-4 pl-10 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] focus:shadow-[0_0_10px_rgba(0,229,255,0.2)] transition-all font-mono"
                  placeholder="admin"
                />
                <svg class="w-4 h-4 text-[var(--ts)] absolute left-3.5 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
              </div>
            </div>
            
            <div class="flex flex-col gap-2">
              <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Password</label>
              <div class="relative">
                <input 
                  type="password" 
                  v-model="password"
                  class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--cyan)] rounded-[4px] h-[44px] px-4 pl-10 pr-4 text-[14px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] focus:shadow-[0_0_12px_rgba(0,229,255,0.3)] transition-all font-mono tracking-widest"
                  placeholder="••••••"
                />
                <svg class="w-4 h-4 text-[var(--cyan)] absolute left-3.5 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                <!-- Cursor line simulation -->
                <div class="absolute right-4 top-1/2 -translate-y-1/2 w-[2px] h-[16px] bg-[var(--cyan)] animate-cursor-blink"></div>
              </div>
            </div>
            
            <div class="flex justify-between items-center mt-2">
              <div class="flex items-center gap-2 cursor-pointer" @click="keepSignedIn = !keepSignedIn">
                <div class="w-3.5 h-3.5 rounded-[2px] border flex items-center justify-center transition-colors" :class="keepSignedIn ? 'bg-[var(--cyan)]/20 border-[var(--cyan)]' : 'border-[var(--ts)]'">
                  <svg v-if="keepSignedIn" class="w-2.5 h-2.5 text-[var(--cyan)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" /></svg>
                </div>
                <span class="font-mono text-[9px] text-[var(--ts)] tracking-wider uppercase">Keep Signed In</span>
              </div>
              <a href="#" class="font-mono text-[9px] text-[var(--cyan)] tracking-wider hover:text-[var(--cyan-soft)] transition-colors uppercase">Forgot?</a>
            </div>
            
            <button 
              type="submit" 
              class="mt-4 w-full h-[48px] rounded-[16px] bg-gradient-to-r from-[rgba(0,229,255,0.3)] to-[rgba(139,92,246,0.25)] border border-[var(--cyan)] shadow-[0_0_16px_rgba(0,229,255,0.4)] hover:shadow-[0_0_25px_rgba(0,229,255,0.6)] transition-all flex items-center justify-center gap-3 relative overflow-hidden group"
              :disabled="loading"
            >
              <div class="absolute inset-0 bg-gradient-to-r from-[var(--cyan)]/0 via-[var(--cyan)]/20 to-[var(--cyan)]/0 -translate-x-[100%] group-hover:translate-x-[100%] transition-transform duration-1000"></div>
              <span class="font-mono text-[10px] text-[var(--tp)] uppercase tracking-[0.08em] font-medium relative z-10">
                {{ loading ? 'Authenticating...' : 'Authenticate' }}
              </span>
              <svg v-if="!loading" class="w-4 h-4 text-[var(--tp)] relative z-10" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
            </button>
            
            <div class="mt-8 flex justify-between items-center border-t border-[var(--ln)] border-dashed pt-4">
              <span class="font-mono text-[8px] text-[var(--td)] uppercase tracking-wider">Session ID: {{ sessionId }}</span>
              <div class="flex items-center gap-1.5">
                <div class="w-1.5 h-1.5 rotate-45 bg-[var(--emerald)] shadow-[0_0_6px_var(--emerald)]"></div>
                <span class="font-mono text-[8px] text-[var(--emerald)] uppercase tracking-widest val-emerald">Secure</span>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const username = ref('admin');
const password = ref('123456');
const keepSignedIn = ref(true);
const loading = ref(false);
const sessionId = ref('');

onMounted(() => {
  sessionId.value = 'NC-' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + Math.random().toString(16).substring(2, 6).toUpperCase();
});

const handleLogin = async () => {
  loading.value = true;
  try {
    await authStore.login({ username: username.value, password: password.value });
    router.push('/dashboard/ai');
  } catch (error) {
    console.error('Login failed', error);
  } finally {
    loading.value = false;
  }
};
</script>
