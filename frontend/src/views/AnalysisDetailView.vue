<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden" v-if="store.currentDetail">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-3">
        <NeuralButton variant="ghost" @click="router.back()">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg></template>
          Back
        </NeuralButton>
        <div class="h-4 w-[1px] bg-line-subtle"></div>
        <h2 class="text-[18px] font-medium text-ice-white m-0">Diagnostic Console</h2>
        <span class="font-mono text-[10px] text-neural-cyan tracking-[0.1em] ml-2">{{ store.currentDetail.task.no }}</span>
      </div>
      
      <div class="flex gap-2">
        <NeuralButton variant="ghost">Export Report</NeuralButton>
        <NeuralButton variant="primary">Start Review</NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-4 gap-3 mb-4 shrink-0">
      <KpiCard label="Confidence" :value="`${(store.currentDetail.summary.confidence! * 100).toFixed(1)}%`" color="cyan" />
      <KpiCard label="Uncertainty" :value="store.currentDetail.summary.uncertainty?.toFixed(2) || '0.00'" :color="store.currentDetail.summary.uncertainty! > 0.5 ? 'amber' : 'emerald'" />
      <KpiCard label="Processing Time" value="1.2s" color="violet" />
      <KpiCard label="Risk Level" :value="store.currentDetail.summary.riskLevel || 'LOW'" :color="store.currentDetail.summary.riskLevel === 'HIGH' ? 'magenta' : 'amber'" />
    </div>
    
    <!-- Main Three Columns -->
    <div class="flex-1 flex gap-3 min-h-0 mb-4 overflow-hidden">
      
      <!-- Left: Panoramic X-Ray Scanner -->
      <div class="flex-[1.45] min-w-0">
        <Panel title="Panoramic X-Ray Scanner" color="cyan">
          <template #meta>
            <div class="flex gap-1 bg-void-black p-0.5 rounded border border-line-subtle">
              <button class="px-2 py-0.5 text-[8px] font-mono bg-neural-cyan/20 text-neural-cyan rounded-xs">OVR</button>
              <button class="px-2 py-0.5 text-[8px] font-mono text-ghost-dim hover:text-ice-white rounded-xs">DET</button>
            </div>
          </template>
          
          <div class="relative w-full h-full rounded bg-[#0A1428] overflow-hidden border-[0.5px] border-line-subtle" style="background: radial-gradient(ellipse at center, #0A1428, #030812)">
            <!-- Background Grid -->
            <div class="absolute inset-0 z-0 opacity-[0.03]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 16px 16px;"></div>
            
            <!-- X-Ray Image Placeholder -->
            <div class="absolute inset-4 z-10 bg-void-black/50 border border-line-subtle/50 rounded flex items-center justify-center overflow-hidden">
              <img :src="store.currentDetail.image.url" alt="X-Ray" class="max-w-full max-h-full object-contain opacity-80 mix-blend-screen" />
              
              <!-- Mock Teeth and Heatmap since no real image is loaded -->
              <div v-if="store.currentDetail.image.url.includes('mock')" class="absolute inset-0 flex items-center justify-center">
                <!-- Example mock tooth shapes -->
                <div class="w-16 h-24 bg-gradient-to-b from-white/20 to-transparent mx-1 rounded-[40%_40%_10%_10%]"></div>
                <div class="w-16 h-24 bg-gradient-to-b from-white/20 to-transparent mx-1 rounded-[40%_40%_10%_10%] relative">
                  <!-- Heatmap spot -->
                  <div class="absolute inset-0 bg-alert-amber/40 blur-[8px] mix-blend-screen rounded-full scale-50"></div>
                </div>
                <div class="w-16 h-24 bg-gradient-to-b from-white/20 to-transparent mx-1 rounded-[40%_40%_10%_10%]"></div>
              </div>
            </div>
            
            <!-- Detection Reticles -->
            <DetectionReticle :x="45" :y="30" :width="15" :height="25" label="G3 · CONF 0.85" color="amber" />
            
            <!-- Scanline -->
            <div class="absolute left-0 right-0 h-[1px] bg-neural-cyan shadow-[0_0_12px_var(--cyan)] z-30 animate-scanline"></div>
            
            <!-- HUD Overlays -->
            <div class="absolute top-2 left-2 z-40 flex flex-col gap-2">
              <HudChip label="NEURAL ACTIVE" color="cyan" />
              <HudChip v-if="store.currentDetail.summary.needsReview" label="HIGH RISK DETECTED" color="magenta" />
            </div>
            
            <div class="absolute bottom-2 left-2 z-40">
              <div class="font-mono text-[9px] text-soft-cyan">X: 1042 Y: 856 ZOOM: 1.0x</div>
            </div>
            
            <!-- Legend Panel -->
            <div class="absolute bottom-2 right-2 z-40 glass-panel p-2 rounded-sm border-line-subtle/50">
              <div class="flex items-center gap-2 mb-1">
                <div class="w-2 h-2 border border-emerald bg-emerald/20"></div>
                <span class="font-mono text-[8px] text-ghost-dim uppercase">G0-G1 Minimal</span>
              </div>
              <div class="flex items-center gap-2 mb-1">
                <div class="w-2 h-2 border border-amber bg-amber/20"></div>
                <span class="font-mono text-[8px] text-ghost-dim uppercase">G2-G3 Moderate</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-2 h-2 border border-magenta bg-magenta/20"></div>
                <span class="font-mono text-[8px] text-ghost-dim uppercase">G4 Severe</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Middle: AI Diagnostic Report -->
      <div class="flex-[0.9] min-w-0">
        <Panel title="AI Diagnostic Report" color="violet">
          <div class="flex flex-col h-full gap-4">
            <!-- Grade Dashboard -->
            <div class="flex items-center gap-6 p-4 bg-void-black border-[0.5px] border-line-subtle rounded corner-cut-tr">
              <div class="relative w-[80px] h-[80px] shrink-0">
                <svg viewBox="0 0 100 100" class="w-full h-full transform -rotate-90">
                  <circle cx="50" cy="50" r="45" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="8" />
                  <circle cx="50" cy="50" r="45" fill="none" :stroke="gradeColorStroke" stroke-width="8" stroke-dasharray="283" :stroke-dashoffset="283 * 0.2" stroke-linecap="round" />
                </svg>
                <div class="absolute inset-0 flex flex-col items-center justify-center">
                  <span class="font-mono text-2xl font-medium" :class="`val-${gradeColorName}`">{{ store.currentDetail.summary.grade }}</span>
                  <span class="font-mono text-[8px] text-ghost-dim tracking-widest uppercase mt-0.5">Grade</span>
                </div>
              </div>
              
              <div class="flex flex-col gap-2 flex-1">
                <div class="flex justify-between items-baseline border-b border-line-subtle/50 pb-1">
                  <span class="font-mono text-[9px] text-ghost-dim uppercase">Lesions</span>
                  <span class="font-mono text-[12px] text-ice-white">2</span>
                </div>
                <div class="flex justify-between items-baseline border-b border-line-subtle/50 pb-1">
                  <span class="font-mono text-[9px] text-ghost-dim uppercase">Max Grade</span>
                  <span class="font-mono text-[12px]" :class="`text-${gradeColorName}`">{{ store.currentDetail.summary.grade }}</span>
                </div>
                <div class="flex justify-between items-baseline pb-1">
                  <span class="font-mono text-[9px] text-ghost-dim uppercase">Action</span>
                  <span class="font-mono text-[10px] text-alert-amber bg-alert-amber/10 px-1 rounded-sm">REVIEW REQ</span>
                </div>
              </div>
            </div>
            
            <!-- Uncertainty -->
            <div class="p-4 bg-alert-amber/5 border-[0.5px] border-alert-amber/30 rounded">
              <UncertaintyBar :value="store.currentDetail.summary.uncertainty || 0" />
              <div class="mt-3 text-[10px] text-alert-amber/80 leading-relaxed font-sans">
                High uncertainty detected due to overlapping shadows in the interproximal region and potential artifacts. Manual review is strongly recommended.
              </div>
            </div>
            
            <!-- Risk Factors -->
            <div class="flex-1 mt-2">
              <h4 class="font-mono text-[9px] text-ghost-dim uppercase tracking-[0.1em] mb-2 border-b border-line-subtle pb-1">Identified Risk Factors</h4>
              <ul class="space-y-1">
                <li v-for="factor in store.currentDetail.summary.riskFactors" :key="factor" class="flex items-start gap-2">
                  <div class="w-1 h-1 bg-critical-magenta rounded-full mt-1.5 shadow-[0_0_4px_var(--magenta)] shrink-0"></div>
                  <span class="text-[11px] text-ice-white">{{ factor }}</span>
                </li>
              </ul>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Right: RAG Knowledge Response -->
      <div class="flex-[0.85] min-w-0">
        <Panel title="Knowledge RAG Response" color="violet">
          <div v-if="store.currentDetail.rag.enabled" class="flex flex-col h-full">
            <div class="flex-1 overflow-auto bg-void-black border-[0.5px] border-line-subtle rounded p-3 text-[11px] text-ice-white leading-relaxed relative">
              <div class="absolute top-0 bottom-0 left-0 w-[2px] bg-knowledge-violet shadow-[0_0_8px_var(--violet)]"></div>
              
              <p class="mb-3">
                Based on the detected <span class="text-alert-amber font-mono">G3</span> lesion characteristics, this involves the inner half of the dentin with localized breakdown<CitationTag id="1" />.
              </p>
              <p>
                Clinical guidelines recommend immediate operative intervention for this severity level to prevent irreversible pulpitis<CitationTag id="2" />. The uncertainty map suggests paying close attention to the distal margin.
              </p>
            </div>
            
            <div class="mt-4">
              <h4 class="font-mono text-[9px] text-ghost-dim uppercase tracking-[0.1em] mb-2">Sources</h4>
              <div class="flex flex-col gap-1.5">
                <div class="flex items-start gap-2 bg-knowledge-violet/5 border border-knowledge-violet/20 p-2 rounded-sm cursor-pointer hover:bg-knowledge-violet/10 transition-colors">
                  <div class="bg-knowledge-violet text-void-black font-mono text-[8px] px-1 py-0.5 rounded-xs mt-0.5">1</div>
                  <div class="flex-1 min-w-0">
                    <div class="text-[10px] text-ice-white truncate">ICDAS Clinical Guide v2</div>
                    <div class="text-[8px] font-mono text-ghost-dim">Page 12</div>
                  </div>
                </div>
                <div class="flex items-start gap-2 bg-knowledge-violet/5 border border-knowledge-violet/20 p-2 rounded-sm cursor-pointer hover:bg-knowledge-violet/10 transition-colors">
                  <div class="bg-knowledge-violet text-void-black font-mono text-[8px] px-1 py-0.5 rounded-xs mt-0.5">2</div>
                  <div class="flex-1 min-w-0">
                    <div class="text-[10px] text-ice-white truncate">Operative Dentistry Protocols</div>
                    <div class="text-[8px] font-mono text-ghost-dim">Page 45</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="flex flex-col items-center justify-center h-full text-ghost-dim">
            <svg class="w-8 h-8 mb-2 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            <span class="text-[11px]">RAG interpretation not generated for this task.</span>
          </div>
        </Panel>
      </div>
      
    </div>
    
    <!-- Timeline Bottom Bar -->
    <div class="shrink-0 mb-4">
      <Timeline :nodes="store.currentDetail.timeline" />
    </div>
  </div>
  
  <!-- Loading State -->
  <div v-else-if="store.loading" class="flex-1 flex flex-col items-center justify-center text-neural-cyan">
    <div class="w-12 h-12 border-2 border-neural-cyan border-t-transparent rounded-full animate-spin mb-4"></div>
    <div class="font-mono text-xs tracking-widest animate-pulse-opacity">NEURAL ENGINE LOADING...</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
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

const route = useRoute();
const router = useRouter();
const store = useAnalysisStore();

onMounted(() => {
  const taskId = parseInt(route.params.taskId as string);
  if (taskId) {
    store.fetchDetail(taskId);
  }
});

const gradeColorName = computed(() => {
  const g = store.currentDetail?.summary.grade || 'G0';
  if (g === 'G0' || g === 'G1') return 'emerald';
  if (g === 'G2') return 'amber';
  if (g === 'G3') return 'magenta';
  return 'violet';
});

const gradeColorStroke = computed(() => {
  return `var(--${gradeColorName.value})`;
});
</script>
