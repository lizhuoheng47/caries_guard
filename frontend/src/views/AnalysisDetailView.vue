<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg" v-if="store.currentDetail">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-3">
        <NeuralButton variant="ghost" @click="router.back()">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg></template>
          Back
        </NeuralButton>
        <div class="h-4 w-[1px] bg-[var(--ln)]"></div>
        <div class="flex flex-col">
          <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase">NEURAL CORE / SCAN / {{ store.currentDetail.task.no }}</span>
          <div class="flex items-center gap-2">
            <h2 class="text-[18px] font-medium text-[var(--tp)] m-0">全景 X 光 · AI 诊断会话</h2>
            <StatusChip status="RUNNING" label="LIVE" />
          </div>
        </div>
      </div>
      
      <div class="flex gap-2">
        <NeuralButton variant="ghost">Export</NeuralButton>
        <NeuralButton variant="primary">
          <template #icon-left><div class="w-1.5 h-1.5 rotate-45 bg-[var(--tp)]"></div></template>
          Submit Review
        </NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-4 gap-3 mb-4 shrink-0">
      <KpiCard label="SCANS TODAY" value="128" trend="12" color="cyan" sparkline="0,12 10,8 20,14 30,5 42,10" />
      <KpiCard label="REVIEW QUEUE" value="07" trend="2" color="amber" sparkline="0,8 10,12 20,6 30,10 42,8" />
      <KpiCard label="HIGH RISK" value="03" color="magenta" sparkline="0,15 10,10 20,12 30,15 42,10" />
      <KpiCard label="AGREEMENT" value="94.2%" trend="0.8" color="emerald" sparkline="0,10 10,12 20,8 30,6 42,4" />
    </div>
    
    <!-- Main Three Columns -->
    <div class="flex-1 flex gap-3 min-h-0 mb-4 overflow-hidden">
      
      <!-- Left: Panoramic X-Ray Scanner (1.45fr) -->
      <div class="flex-[1.45] min-w-0">
        <Panel title="Panoramic X-Ray" color="cyan">
          <template #meta>
            <div class="flex gap-1 bg-[rgba(3,8,18,0.5)] p-0.5 rounded-[2px] border border-[var(--ln)]">
              <button class="px-2 py-0.5 text-[8px] font-mono bg-[var(--cyan)]/20 text-[var(--cyan)] rounded-[2px] glow-cyan">OVR</button>
              <button class="px-2 py-0.5 text-[8px] font-mono text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">DET</button>
              <button class="px-2 py-0.5 text-[8px] font-mono text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">MSK</button>
              <button class="px-2 py-0.5 text-[8px] font-mono text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">HT</button>
            </div>
          </template>
          
          <div class="relative w-full h-full rounded-[4px] bg-[#0A1428] overflow-hidden border-[0.5px] border-[var(--ln)]" style="background: radial-gradient(ellipse at center, #0A1428, #030812)">
            <div class="absolute inset-0 z-0 opacity-[0.03]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 16px 16px;"></div>
            
            <div class="absolute inset-4 z-10 bg-black/50 border border-[var(--ln)] rounded-[4px] flex items-center justify-center overflow-hidden">
              <img v-if="!store.currentDetail.image.url.includes('mock')" :src="store.currentDetail.image.url" alt="X-Ray" class="max-w-full max-h-full object-contain opacity-80 mix-blend-screen" />
              
              <div v-else class="absolute inset-0 flex items-center justify-center gap-1">
                <div v-for="i in 8" :key="i" class="w-12 h-32 bg-gradient-to-b from-white/20 to-transparent rounded-[40%_40%_10%_10%] relative">
                  <div v-if="i === 4" class="absolute inset-0 bg-[var(--amber)]/40 blur-[8px] mix-blend-screen rounded-full scale-[0.6] top-4"></div>
                  <div v-if="i === 5" class="absolute inset-0 bg-[var(--magenta)]/40 blur-[8px] mix-blend-screen rounded-full scale-[0.5] top-8"></div>
                </div>
              </div>
            </div>
            
            <DetectionReticle :x="42" :y="30" :width="10" :height="35" label="G3 · 0.72" color="amber" />
            <DetectionReticle :x="55" :y="35" :width="10" :height="30" label="G4 · 0.58" color="magenta" />
            
            <div class="absolute left-0 right-0 h-[1px] bg-[var(--cyan)] shadow-[0_0_12px_var(--cyan)] z-30 animate-scanline"></div>
            
            <div class="absolute top-2 left-2 z-40 flex flex-col gap-2">
              <HudChip label="NEURAL ACTIVE" color="cyan" />
              <HudChip label="2 LESIONS" color="amber" />
            </div>
            
            <div class="absolute bottom-2 left-2 z-40">
              <div class="font-mono text-[8px] text-[var(--cyan-soft)]">X: 0420 Y: 0312</div>
            </div>
            
            <div class="absolute bottom-2 right-2 z-40 glass-panel p-2 rounded-[4px] border-[var(--ln)]">
              <div class="flex items-center gap-2 mb-1">
                <div class="w-2 h-2 bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]"></div>
                <span class="font-mono text-[8px] text-[var(--ts)] uppercase">G3 Moderate</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-2 h-2 bg-[var(--magenta)] shadow-[0_0_4px_var(--magenta)]"></div>
                <span class="font-mono text-[8px] text-[var(--ts)] uppercase">G4 Deep</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Middle: AI Diagnostic Report (0.9fr) -->
      <div class="flex-[0.9] min-w-0">
        <Panel title="AI Diagnostic" color="violet">
          <div class="flex flex-col h-full gap-4">
            <div class="flex items-center gap-4 p-4 border-[0.5px] border-[var(--ln)] rounded-[4px] corner-cut-tr" style="background: rgba(10,20,40,0.5)">
              <div class="relative w-[80px] h-[80px] shrink-0">
                <svg viewBox="0 0 100 100" class="w-full h-full transform -rotate-90 drop-shadow-[0_0_8px_var(--magenta)]">
                  <circle cx="50" cy="50" r="45" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="6" />
                  <circle cx="50" cy="50" r="45" fill="none" stroke="url(#grade-grad)" stroke-width="6" stroke-dasharray="283" :stroke-dashoffset="283 * 0.2" stroke-linecap="round" />
                  <defs>
                    <linearGradient id="grade-grad" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stop-color="var(--cyan)" />
                      <stop offset="50%" stop-color="var(--amber)" />
                      <stop offset="100%" stop-color="var(--magenta)" />
                    </linearGradient>
                  </defs>
                </svg>
                <div class="absolute inset-0 flex flex-col items-center justify-center">
                  <span class="font-mono text-[24px] font-medium val-magenta">G3</span>
                  <span class="font-mono text-[8px] text-[var(--td)] tracking-widest uppercase mt-0.5">Grade</span>
                </div>
              </div>
              
              <div class="flex flex-col gap-2 flex-1 pl-2">
                <div class="flex justify-between items-baseline border-b border-[var(--ln)] pb-1">
                  <span class="font-mono text-[9px] text-[var(--td)] uppercase tracking-wider">Lesions</span>
                  <span class="font-mono text-[12px] text-[var(--tp)]">02</span>
                </div>
                <div class="flex justify-between items-baseline border-b border-[var(--ln)] pb-1">
                  <span class="font-mono text-[9px] text-[var(--td)] uppercase tracking-wider">Max</span>
                  <span class="font-mono text-[12px] val-magenta">G4</span>
                </div>
                <div class="flex justify-between items-baseline pb-1">
                  <span class="font-mono text-[9px] text-[var(--td)] uppercase tracking-wider">Action</span>
                  <span class="font-mono text-[10px] text-[var(--amber)] val-amber">REVIEW</span>
                </div>
              </div>
            </div>
            
            <div class="p-4 border-[0.5px] border-[var(--amber)]/30 rounded-[4px] relative bg-[var(--amber)]/5">
              <div class="absolute top-0 right-0 w-[8px] h-[8px] border-t border-r border-[var(--amber)]/50"></div>
              <UncertaintyBar :value="0.72" />
              <div class="mt-4 text-[10px] text-[var(--amber)] leading-relaxed flex flex-col gap-1">
                <span class="font-mono tracking-widest opacity-80">TRIGGER: G3/G4 边界分歧</span>
                <span class="opacity-90">自动升级复核，建议确认病变深度是否到达内层牙本质。</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Right: RAG Knowledge Response (0.85fr) -->
      <div class="flex-[0.85] min-w-0">
        <Panel title="Knowledge RAG" color="violet">
          <div class="flex flex-col h-full relative">
            <div class="absolute top-0 right-0 font-mono text-[8px] text-[var(--td)] uppercase text-right">caries-<br/>v1.0</div>
            
            <div class="mt-8 mb-4 p-4 rounded-[4px] border border-[var(--violet)]/20 bg-[var(--violet)]/5 border-l-2 border-l-[var(--violet)] relative">
              <div class="absolute inset-0 bg-gradient-to-r from-[var(--violet)]/10 to-transparent opacity-50 pointer-events-none"></div>
              <p class="text-[12px] text-[var(--tp)] leading-relaxed">
                G3 级龋病表现为牙本质中层受累<CitationTag id="1" />，建议及时充填以阻止向牙髓扩展<CitationTag id="2" />。
              </p>
            </div>
            
            <div class="flex flex-col gap-2 flex-1 overflow-auto">
              <div class="flex items-center justify-between p-2 rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)]">
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-xs flex items-center justify-center font-mono text-[8px] text-[var(--violet)]">1</div>
                  <span class="text-[11px] text-[var(--ts)]">龋病分级诊断指南</span>
                </div>
                <span class="font-mono text-[8px] text-[var(--td)]">P.42</span>
              </div>
              <div class="flex items-center justify-between p-2 rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)]">
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-xs flex items-center justify-center font-mono text-[8px] text-[var(--violet)]">2</div>
                  <span class="text-[11px] text-[var(--ts)]">临床路径手册</span>
                </div>
                <span class="font-mono text-[8px] text-[var(--td)]">P.18</span>
              </div>
              <div class="flex items-center justify-between p-2 rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)]">
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-xs flex items-center justify-center font-mono text-[8px] text-[var(--violet)]">3</div>
                  <span class="text-[11px] text-[var(--ts)]">龋病流行病学研究</span>
                </div>
                <span class="font-mono text-[8px] text-[var(--td)]">P.07</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
    </div>
    
    <!-- Timeline Bottom Bar -->
    <div class="shrink-0 mb-4 flex">
      <div class="flex items-center">
        <div class="w-[3px] h-[16px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)] mr-2"></div>
        <span class="font-mono text-[10px] text-[var(--tp)] uppercase tracking-wider mr-6">AI Trace</span>
      </div>
      <div class="flex-1">
        <Timeline :nodes="[
          { label: '上传', status: 'DONE', time: '14:02' },
          { label: '创建', status: 'DONE', time: '14:02' },
          { label: '推理', status: 'DONE', time: '14:02' },
          { label: 'RAG', status: 'DONE', time: '14:02' },
          { label: '复核', status: 'ACTIVE', time: 'LIVE' },
          { label: '反馈', status: 'FUTURE' }
        ]" />
      </div>
    </div>
  </div>
  
  <!-- Loading State -->
  <div v-else-if="store.loading" class="flex-1 flex flex-col items-center justify-center">
    <div class="w-16 h-16 rounded-full border border-[var(--cyan)]/30 border-t-[var(--cyan)] animate-spin mb-4 shadow-[0_0_15px_rgba(0,229,255,0.2)]"></div>
    <div class="font-mono text-[12px] text-[var(--cyan)] tracking-[0.2em] animate-pulse-opacity">NEURAL ENGINE INITIALIZING...</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAnalysisStore } from '../stores/analysis';
import NeuralButton from '../components/shared/NeuralButton.vue';
import KpiCard from '../components/shared/KpiCard.vue';
import Panel from '../components/shared/Panel.vue';
import DetectionReticle from '../components/shared/DetectionReticle.vue';
import HudChip from '../components/shared/HudChip.vue';
import UncertaintyBar from '../components/shared/UncertaintyBar.vue';
import CitationTag from '../components/shared/CitationTag.vue';
import Timeline from '../components/shared/Timeline.vue';
import StatusChip from '../components/shared/StatusChip.vue';

const route = useRoute();
const router = useRouter();
const store = useAnalysisStore();

onMounted(() => {
  const taskId = parseInt(route.params.taskId as string) || 1;
  store.fetchDetail(taskId);
});
</script>
