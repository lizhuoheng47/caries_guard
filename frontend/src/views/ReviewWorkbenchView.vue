<template>
  <div class="flex h-full overflow-hidden page-bg">
    <!-- Review Queue Sidebar -->
    <div class="w-[240px] shrink-0 border-r border-[var(--ln)] flex flex-col" style="background: rgba(3,8,18,0.4)">
      <div class="p-4 border-b border-[var(--ln)]">
        <div class="flex items-center justify-between mb-2">
          <h2 class="font-mono text-[11px] text-[var(--amber)] val-amber uppercase tracking-widest">Review Queue</h2>
          <div class="px-1.5 py-0.5 rounded-xs bg-[var(--amber)]/20 text-[var(--amber)] font-mono text-[8px] border border-[var(--amber)]">07</div>
        </div>
        <!-- Search/Filter -->
        <div class="relative">
          <input type="text" placeholder="Filter..." class="w-full h-[28px] bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] px-2 text-[11px] text-[var(--tp)] font-mono focus:border-[var(--amber)] focus:outline-none" />
        </div>
      </div>
      
      <div class="flex-1 overflow-y-auto p-2 flex flex-col gap-2">
        <!-- Active Card -->
        <div v-if="workbenchData" class="p-3 border rounded-[4px] border-[var(--amber)] bg-[var(--amber)]/5 shadow-[0_0_12px_rgba(255,181,71,0.15)] relative cursor-pointer">
          <div class="absolute left-0 top-2 bottom-2 w-[2px] bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]"></div>
          <div class="flex justify-between items-start mb-2 pl-1">
            <span class="font-mono text-[10px] text-[var(--amber)]">{{ workbenchData.task.taskNo || 'T-' + workbenchData.task.taskId }}</span>
            <span class="font-mono text-[8px] text-[var(--td)]">{{ new Date(workbenchData.task.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
          </div>
          <div class="flex items-center gap-2 pl-1">
            <GradeBadge :grade="workbenchData.aiResult.gradingLabel" />
            <div class="font-mono text-[9px] text-[var(--ts)] flex-1">{{ workbenchData.caseInfo.caseNo }}</div>
            <span class="font-mono text-[9px] text-[var(--amber)]">UC {{ workbenchData.aiResult.uncertaintyScore.toFixed(2) }}</span>
          </div>
        </div>
        
        <!-- Inactive Cards -->
        <div v-for="i in 6" :key="i" class="p-3 border rounded-[4px] border-[var(--ln)] bg-[rgba(10,20,40,0.3)] hover:bg-[rgba(10,20,40,0.6)] cursor-pointer transition-colors">
          <div class="flex justify-between items-start mb-2">
            <span class="font-mono text-[10px] text-[var(--ts)]">T-2026041800{{ i+1 }}</span>
            <span class="font-mono text-[8px] text-[var(--td)]">13:{{ 50-i }}</span>
          </div>
          <div class="flex items-center gap-2">
            <GradeBadge :grade="i % 2 === 0 ? 'G2' : 'G4'" />
            <div class="font-mono text-[9px] text-[var(--td)] flex-1">P-100{{ 2+i }}</div>
            <span class="font-mono text-[9px] text-[var(--ts)]">UC 0.{{ 40+i*5 }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Main Workspace -->
    <div class="flex-1 flex flex-col min-w-0 p-4 lg:p-6">
      <div class="flex items-center justify-between mb-4 shrink-0">
        <div class="flex items-center gap-3">
          <h2 class="text-[18px] font-medium text-[var(--tp)] m-0">复核工作台</h2>
          <span class="font-mono text-[10px] text-[var(--amber)] tracking-[0.1em]" v-if="workbenchData">{{ workbenchData.task.taskNo || 'T-' + workbenchData.task.taskId }}</span>
        </div>
      </div>
      
      <div class="flex-1 flex gap-3 min-h-0 overflow-hidden">
        
        <!-- Left: Image Scanner -->
        <div class="flex-[1.2] min-w-0">
          <Panel title="Medical Image" color="cyan">
            <div class="relative w-full h-full rounded-[4px] bg-[#0A1428] overflow-hidden border-[0.5px] border-[var(--ln)]" style="background: radial-gradient(ellipse at center, #0A1428, #030812)">
              <div class="absolute inset-0 z-0 opacity-[0.03]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 16px 16px;"></div>
              
              <div class="absolute inset-4 z-10 bg-black/50 border border-[var(--ln)] rounded-[4px] flex items-center justify-center overflow-hidden">
                <div class="absolute inset-0 flex items-center justify-center gap-1" v-if="!workbenchData || workbenchData.image.imageUrl.includes('mock')">
                  <div class="w-12 h-32 bg-gradient-to-b from-white/20 to-transparent rounded-[40%_40%_10%_10%]"></div>
                  <div class="w-12 h-32 bg-gradient-to-b from-white/20 to-transparent rounded-[40%_40%_10%_10%] relative">
                    <div class="absolute inset-0 bg-[var(--amber)]/20 blur-[8px] mix-blend-screen rounded-full scale-[0.6] top-4"></div>
                  </div>
                </div>
                <img v-else :src="workbenchData.image.imageUrl" alt="X-Ray" class="max-w-full max-h-full object-contain opacity-80 mix-blend-screen" />
              </div>
              
              <!-- AI Original Box (50% opacity) -->
              <div class="absolute pointer-events-none opacity-50" style="left: 45%; top: 30%; width: 10%; height: 35%;">
                <div class="absolute inset-0 border border-[var(--amber)]"></div>
                <div class="absolute -top-4 left-1/2 -translate-x-1/2 bg-[var(--amber)] px-1 py-0.5 rounded-xs"><span class="font-mono text-[7px] text-[var(--void)]">AI: G3</span></div>
              </div>
              
              <!-- Doctor Modified Box (Emerald Dashed) -->
              <div class="absolute pointer-events-none" style="left: 45%; top: 35%; width: 10%; height: 25%;">
                <div class="absolute inset-0 border-[1.5px] border-dashed border-[var(--emerald)] shadow-[0_0_8px_rgba(0,255,163,0.3)]"></div>
                <div class="absolute -top-4 left-1/2 -translate-x-1/2 bg-[var(--emerald)] px-1 py-0.5 rounded-xs shadow-[0_0_8px_var(--emerald)]"><span class="font-mono text-[7px] text-[var(--void)] font-bold">DOC: G2</span></div>
              </div>
              
              <div class="absolute top-2 left-2 z-40 flex flex-col gap-2">
                <HudChip label="AI ORIGINAL · 2 LESIONS" color="amber" />
                <HudChip label="DOCTOR · 1 MODIFIED" color="emerald" />
              </div>
            </div>
          </Panel>
        </div>
        
        <!-- Right: Doctor Modification Panel -->
        <div class="flex-1 min-w-0 flex flex-col gap-3">
          <Panel title="Modification Review" color="emerald">
            <div class="flex flex-col h-full gap-4">
              
              <!-- Compare Card -->
              <div class="flex items-center justify-between p-3 border border-[var(--ln)] rounded-[4px] bg-[rgba(10,20,40,0.5)]">
                <div class="flex flex-col items-center flex-1">
                  <span class="font-mono text-[8px] text-[var(--td)] mb-1">AI GRADE</span>
                  <span class="font-mono text-[18px] text-[var(--amber)] val-amber">{{ workbenchData?.aiResult.gradingLabel || 'G3' }}</span>
                </div>
                <svg class="w-5 h-5 text-[var(--td)] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                <div class="flex flex-col items-center flex-1">
                  <span class="font-mono text-[8px] text-[var(--emerald)] mb-1">DOCTOR</span>
                  <span class="font-mono text-[18px] text-[var(--emerald)] val-emerald">{{ selectedGrade || 'G2' }}</span>
                </div>
              </div>
              
              <!-- Grade Selector -->
              <div>
                <span class="font-mono text-[9px] text-[var(--td)] tracking-widest uppercase mb-2 block">Correct Grade</span>
                <div class="flex gap-2">
                  <button v-for="g in (workbenchData?.reviewOptions.gradeOptions || ['G0','G1','G2','G3','G4'])" :key="g" 
                          @click="selectedGrade = g"
                          class="flex-1 py-2 rounded-[3px] border font-mono text-[10px] transition-colors relative"
                          :class="g === selectedGrade ? 'bg-[var(--emerald)]/20 border-[var(--emerald)] text-[var(--emerald)] shadow-[0_0_8px_rgba(0,255,163,0.2)]' : g === workbenchData?.aiResult.gradingLabel ? 'border-[var(--amber)]/30 text-[var(--amber)] opacity-50' : 'border-[var(--ln)] text-[var(--td)] hover:text-[var(--ts)] bg-[rgba(3,8,18,0.5)]'">
                    {{ g }}
                    <div v-if="g === selectedGrade" class="absolute -top-1.5 -right-1.5 w-3.5 h-3.5 bg-[var(--emerald)] rounded-full flex items-center justify-center shadow-[0_0_8px_var(--emerald)]">
                      <svg class="w-2.5 h-2.5 text-[var(--void)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" /></svg>
                    </div>
                  </button>
                </div>
              </div>
              
              <!-- Reason Tags -->
              <div>
                <span class="font-mono text-[9px] text-[var(--td)] tracking-widest uppercase mb-2 block">Reason</span>
                <div class="flex flex-wrap gap-2">
                  <button v-for="tag in (workbenchData?.reviewOptions.reasonTags || ['病灶范围过估', '边界误判', '影像伪影', '深度判断偏差', '其他'])" :key="tag"
                          @click="toggleTag(tag)"
                          class="px-3 py-1.5 rounded-[3px] border font-mono text-[9px] transition-colors"
                          :class="selectedTags.includes(tag) ? 'border-[var(--cyan)] bg-[var(--cyan)]/20 text-[var(--cyan)] shadow-[0_0_8px_rgba(0,229,255,0.2)]' : 'border-[var(--ln)] bg-[rgba(3,8,18,0.5)] text-[var(--td)] hover:text-[var(--ts)]'">
                    {{ tag }}
                  </button>
                </div>
              </div>
              
              <!-- Clinical Notes -->
              <div class="flex-1 flex flex-col min-h-[100px]">
                <span class="font-mono text-[9px] text-[var(--td)] tracking-widest uppercase mb-2 block">Clinical Note</span>
                <div class="flex-1 relative">
                  <textarea 
                    v-model="clinicalNote"
                    class="w-full h-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[4px] p-3 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-colors resize-none"
                    placeholder="Enter observations..."
                  ></textarea>
                </div>
              </div>
              
              <!-- Actions -->
              <div class="grid grid-cols-3 gap-2 mt-auto">
                <NeuralButton variant="ghost" class="text-[var(--td)] border-[var(--td)]/30 hover:border-[var(--td)]">SAVE DRAFT</NeuralButton>
                <NeuralButton variant="danger">2ND OPINION</NeuralButton>
                <NeuralButton variant="success" @click="submitReview" :loading="submitting">SUBMIT & LOG</NeuralButton>
              </div>
            </div>
          </Panel>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Panel from '../components/shared/Panel.vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import GradeBadge from '../components/shared/GradeBadge.vue';
import HudChip from '../components/shared/HudChip.vue';
import { reviewApi } from '../api/review';

const route = useRoute();
const router = useRouter();

const workbenchData = ref<any>(null);
const selectedGrade = ref('G2');
const selectedTags = ref<string[]>([]);
const clinicalNote = ref('');
const submitting = ref(false);

const fetchWorkbench = async () => {
  const taskId = parseInt(route.params.taskId as string) || 1;
  try {
    const res = await reviewApi.getReviewWorkbench(taskId);
    workbenchData.value = res.data;
    if (res.data.doctorDraft) {
      selectedGrade.value = res.data.doctorDraft.revisedGrade || res.data.aiResult.gradingLabel;
      selectedTags.value = res.data.doctorDraft.reasonTags || [];
      clinicalNote.value = res.data.doctorDraft.note || '';
    } else {
      selectedGrade.value = res.data.aiResult.gradingLabel;
    }
  } catch (error) {
    console.error('Failed to fetch review workbench', error);
  }
};

const toggleTag = (tag: string) => {
  const index = selectedTags.value.indexOf(tag);
  if (index > -1) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tag);
  }
};

const submitReview = async () => {
  if (!workbenchData.value) return;
  submitting.value = true;
  try {
    await reviewApi.submitReview({
      taskId: workbenchData.value.task.taskId,
      revisedGrade: selectedGrade.value,
      reasonTags: selectedTags.value,
      note: clinicalNote.value
    });
    router.push('/analysis');
  } catch (error) {
    console.error('Failed to submit review', error);
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  fetchWorkbench();
});
</script>
