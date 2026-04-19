<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">AI CORE / <span class="text-[var(--cyan)]">CASES</span></span>
        <h2 class="text-[20px] font-medium text-[var(--tp)] m-0">影像扫描</h2>
      </div>
      
      <div class="flex items-center gap-3">
        <!-- Search -->
        <div class="relative w-[260px]">
          <input 
            type="text" 
            placeholder="Search patient ID or name..." 
            class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[32px] pl-9 pr-3 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] focus:shadow-[0_0_8px_rgba(0,229,255,0.2)] transition-all font-mono placeholder:text-[var(--td)]"
          />
          <svg class="w-4 h-4 text-[var(--ts)] absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
        </div>
        
        <!-- Filters -->
        <div class="flex gap-1 border border-[var(--ln)] rounded-[3px] p-0.5" style="background: rgba(3,8,18,0.5);">
          <button class="px-3 py-1 font-mono text-[9px] bg-[var(--cyan)] text-[var(--void)] rounded-[2px] shadow-[0_0_8px_var(--cyan)] glow-cyan">ALL</button>
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">PENDING</button>
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">ANALYZED</button>
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">REVIEWED</button>
        </div>
        
        <NeuralButton variant="primary" @click="showNewCase = true">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg></template>
          New Case
        </NeuralButton>
      </div>
    </div>
    
    <!-- Case Grid -->
    <div class="flex-1 overflow-y-auto min-h-0 pr-1">
      <div class="grid grid-cols-3 gap-3">
        <div 
          v-for="c in store.tasks.items" 
          :key="c.id" 
          class="glass-panel rounded-md overflow-hidden cursor-pointer hover:border-[var(--cyan)]/40 transition-all group relative"
          @click="router.push(`/analysis/${c.id}`)"
        >
          <!-- Left accent -->
          <div class="absolute left-0 top-0 bottom-0 w-[2px] z-20" :class="getGradeAccent(c.grade)"></div>
          
          <!-- X-ray Thumbnail -->
          <div class="aspect-[16/9] bg-[#0A1428] relative overflow-hidden border-b border-[var(--ln)]">
            <!-- Grid texture -->
            <div class="absolute inset-0 opacity-[0.04]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 12px 12px;"></div>
            
            <!-- Simulated X-ray visual -->
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="flex gap-0.5 opacity-50">
                <div v-for="t in 6" :key="t" class="w-5 h-14 bg-gradient-to-b from-white/20 to-transparent rounded-[40%_40%_10%_10%]"></div>
              </div>
            </div>
            
            <!-- Gradient overlay -->
            <div class="absolute inset-0 bg-gradient-to-t from-[#030812] via-transparent to-transparent"></div>
            
            <!-- Top right HUD chips -->
            <div class="absolute top-2 right-2 flex flex-col gap-1 z-10">
              <div class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-[2px] border" :class="getGradeChipClass(c.grade)">
                <span class="font-mono text-[8px] font-medium">{{ c.grade }}</span>
              </div>
            </div>
            
            <!-- Bottom left scan info -->
            <div class="absolute bottom-2 left-2 z-10 flex items-center gap-1.5">
              <div class="w-1.5 h-1.5 rounded-full bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div>
              <span class="font-mono text-[8px] text-[var(--cyan-soft)]">{{ new Date(c.createdAt).toLocaleDateString() }}</span>
            </div>
            
            <!-- Scanline on hover -->
            <div class="absolute left-0 right-0 h-[1px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)] opacity-0 group-hover:opacity-100 transition-opacity z-10 animate-scanline"></div>
          </div>
          
          <!-- Card Body -->
          <div class="p-3 flex flex-col gap-2">
            <!-- Patient Info -->
            <div class="flex justify-between items-start">
              <div class="flex flex-col">
                <span class="text-[12px] text-[var(--tp)] font-medium">{{ c.patientName || c.patientId }}</span>
                <span class="font-mono text-[9px] text-[var(--td)]">{{ c.patientId }} | {{ c.no }}</span>
              </div>
              <span class="font-mono text-[8px] text-[var(--td)] uppercase">{{ c.caseNo }}</span>
            </div>
            
            <!-- Bottom: Status + Grade + Uncertainty mini bar -->
            <div class="flex items-center justify-between gap-2 pt-1 border-t border-[var(--ln)]">
              <!-- Status -->
              <div class="flex items-center gap-1.5">
                <div class="w-1 h-1 rounded-full animate-pulse-opacity" :class="getStatusDotClass(c.status)"></div>
                <span class="font-mono text-[8px] uppercase tracking-[0.1em]" :class="getStatusTextClass(c.status)">{{ c.status }}</span>
              </div>
              
              <!-- Uncertainty mini bar -->
              <div class="flex items-center gap-2 flex-1 max-w-[120px]">
                <div class="flex-1 h-[3px] bg-[var(--void)] border border-[var(--ln)] rounded-full overflow-hidden">
                  <div class="h-full" :style="{ width: `${(c.uncertainty || 0) * 100}%` }" :class="(c.uncertainty || 0) > 0.35 ? 'bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]' : 'bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]'"></div>
                </div>
                <span class="font-mono text-[8px] tabular-nums" :class="(c.uncertainty || 0) > 0.35 ? 'text-[var(--amber)]' : 'text-[var(--ts)]'">{{ (c.uncertainty || 0).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- New Case Drawer (Simplified) -->
    <Teleport to="body">
      <Transition name="fade-up">
        <div v-if="showNewCase" class="fixed inset-0 z-50 flex items-center justify-center" @click.self="showNewCase = false">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="glass-panel w-[480px] p-8 rounded-md relative z-10 shadow-[0_20px_60px_rgba(0,0,0,0.6)]">
            <!-- Reticle corners -->
            <div class="absolute top-0 left-0 w-[10px] h-[10px] border-t border-l border-[var(--cyan)]/60"></div>
            <div class="absolute top-0 right-0 w-[10px] h-[10px] border-t border-r border-[var(--cyan)]/60"></div>
            <div class="absolute bottom-0 left-0 w-[10px] h-[10px] border-b border-l border-[var(--cyan)]/60"></div>
            <div class="absolute bottom-0 right-0 w-[10px] h-[10px] border-b border-r border-[var(--cyan)]/60"></div>
            
            <div class="flex items-center gap-3 mb-1">
              <div class="w-[3px] h-[16px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]"></div>
              <h3 class="text-[14px] font-medium text-[var(--tp)] m-0">New Case</h3>
            </div>
            <div class="font-mono text-[8px] text-[var(--td)] tracking-[0.15em] mb-8 pl-[15px]">REGISTER NEW PATIENT SCAN</div>
            
            <form class="flex flex-col gap-5" @submit.prevent="showNewCase = false">
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1.5">
                  <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Patient ID</label>
                  <input type="text" class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[36px] px-3 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all font-mono" placeholder="P-XXXX" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Age</label>
                  <input type="number" class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[36px] px-3 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all font-mono" placeholder="45" />
                </div>
              </div>
              
              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Gender</label>
                <div class="flex gap-2">
                  <button type="button" class="flex-1 py-2 rounded-[3px] border border-[var(--cyan)] bg-[var(--cyan)]/20 text-[var(--cyan)] font-mono text-[10px] shadow-[0_0_8px_rgba(0,229,255,0.2)]">Male</button>
                  <button type="button" class="flex-1 py-2 rounded-[3px] border border-[var(--ln)] text-[var(--td)] font-mono text-[10px] hover:text-[var(--ts)]">Female</button>
                </div>
              </div>
              
              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">Chief Complaint</label>
                <textarea class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] p-3 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all resize-none h-[60px]" placeholder="Enter patient complaint..."></textarea>
              </div>
              
              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[9px] text-[var(--ts)] tracking-widest uppercase">X-Ray Upload</label>
                <div class="border border-dashed border-[var(--cyan)]/30 rounded-[4px] p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-[var(--cyan)]/5 transition-colors">
                  <svg class="w-6 h-6 text-[var(--cyan)] opacity-50 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                  <span class="text-[11px] text-[var(--ts)]">Drop X-ray image here</span>
                  <span class="text-[9px] text-[var(--td)] mt-1">PNG, JPG, DICOM</span>
                </div>
              </div>
              
              <div class="flex gap-2 mt-2">
                <button type="button" @click="showNewCase = false" class="flex-1 py-2.5 rounded-[3px] border border-[var(--ln)] text-[var(--td)] font-mono text-[9px] uppercase tracking-wider hover:text-[var(--ts)] transition-colors">Cancel</button>
                <button type="submit" class="flex-1 py-2.5 rounded-[3px] bg-gradient-to-r from-[var(--cyan)]/20 to-[var(--violet)]/20 border border-[var(--cyan)] text-[var(--tp)] font-mono text-[9px] uppercase tracking-wider shadow-[0_0_12px_rgba(0,229,255,0.3)] hover:shadow-[0_0_20px_rgba(0,229,255,0.5)] transition-all">Create & Analyze</button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import NeuralButton from '../components/shared/NeuralButton.vue';
import { useAnalysisStore } from '../stores/analysis';

const router = useRouter();
const showNewCase = ref(false);
const store = useAnalysisStore();

onMounted(() => {
  store.fetchTasks({ pageNum: 1, pageSize: 12 });
});

const getGradeAccent = (grade: string) => {
  switch (grade) {
    case 'G0': return 'bg-[var(--emerald)]';
    case 'G1': return 'bg-[var(--cyan)]';
    case 'G2': return 'bg-[var(--amber)]';
    case 'G3': return 'bg-[var(--magenta)]';
    case 'G4': return 'bg-[var(--violet)]';
    default: return 'bg-[var(--td)]';
  }
};

const getGradeChipClass = (grade: string) => {
  switch (grade) {
    case 'G0': return 'bg-[var(--emerald)]/15 border-[var(--emerald)]/40 text-[var(--emerald)]';
    case 'G1': return 'bg-[var(--cyan)]/15 border-[var(--cyan)]/40 text-[var(--cyan)]';
    case 'G2': return 'bg-[var(--amber)]/15 border-[var(--amber)]/40 text-[var(--amber)]';
    case 'G3': return 'bg-[var(--magenta)]/15 border-[var(--magenta)]/40 text-[var(--magenta)]';
    case 'G4': return 'bg-[var(--violet)]/15 border-[var(--violet)]/40 text-[var(--violet)]';
    default: return 'bg-[var(--td)]/15 border-[var(--td)]/40 text-[var(--td)]';
  }
};

const getStatusDotClass = (status: string) => {
  switch (status) {
    case 'DONE': return 'bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]';
    case 'RUNNING': return 'bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]';
    case 'REVIEW': return 'bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]';
    case 'FAILED': return 'bg-[var(--magenta)] shadow-[0_0_8px_var(--magenta)]';
    default: return 'bg-[var(--td)]';
  }
};

const getStatusTextClass = (status: string) => {
  switch (status) {
    case 'DONE': return 'text-[var(--emerald)]';
    case 'RUNNING': return 'text-[var(--cyan)]';
    case 'REVIEW': return 'text-[var(--amber)]';
    case 'FAILED': return 'text-[var(--magenta)]';
    default: return 'text-[var(--td)]';
  }
};
</script>
