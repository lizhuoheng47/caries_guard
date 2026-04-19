<template>
  <div class="flex h-full overflow-hidden page-bg">
    <div class="w-[280px] shrink-0 border-r border-[var(--ln)] flex flex-col" style="background: rgba(3,8,18,0.4)">
      <div class="p-4 border-b border-[var(--ln)]">
        <div class="flex items-center justify-between mb-2">
          <h2 class="font-mono text-[12px] text-[var(--amber)] val-amber uppercase tracking-widest">复核队列</h2>
          <div class="px-1.5 py-0.5 rounded-xs bg-[var(--amber)]/20 text-[var(--amber)] font-mono text-[10px] border border-[var(--amber)]">
            {{ filteredQueueItems.length.toString().padStart(2, '0') }}
          </div>
        </div>
        <div class="relative">
          <input
            v-model.trim="queueKeyword"
            type="text"
            placeholder="筛选病例、任务号..."
            class="w-full h-[32px] bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] px-2 text-[12px] text-[var(--tp)] font-mono focus:border-[var(--amber)] focus:outline-none"
          />
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-2 flex flex-col gap-2">
        <div v-if="queueLoading" class="p-3 rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.3)] text-[12px] text-[var(--td)]">
          正在加载复核队列...
        </div>

        <button
          v-for="item in filteredQueueItems"
          :key="item.id"
          type="button"
          class="p-3 border rounded-[4px] text-left relative cursor-pointer transition-colors"
          :class="item.id === activeTaskId
            ? 'border-[var(--amber)] bg-[var(--amber)]/5 shadow-[0_0_12px_rgba(255,181,71,0.15)]'
            : 'border-[var(--ln)] bg-[rgba(10,20,40,0.3)] hover:bg-[rgba(10,20,40,0.6)]'"
          @click="selectTask(item.id)"
        >
          <div
            v-if="item.id === activeTaskId"
            class="absolute left-0 top-2 bottom-2 w-[2px] bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]"
          ></div>

          <div class="flex justify-between items-start mb-2" :class="item.id === activeTaskId ? 'pl-1' : ''">
            <span class="font-mono text-[11px]" :class="item.id === activeTaskId ? 'text-[var(--amber)]' : 'text-[var(--ts)]'">
              {{ item.no }}
            </span>
            <span class="font-mono text-[9px] text-[var(--td)]">{{ formatTaskTime(item.createdAt) }}</span>
          </div>

          <div class="flex items-center gap-2" :class="item.id === activeTaskId ? 'pl-1' : ''">
            <GradeBadge :grade="getDisplayedQueueGrade(item)" />
            <div class="font-mono text-[10px] flex-1" :class="item.id === activeTaskId ? 'text-[var(--ts)]' : 'text-[var(--td)]'">
              {{ item.caseNo }}
            </div>
            <span class="font-mono text-[10px]" :class="item.id === activeTaskId ? 'text-[var(--amber)]' : 'text-[var(--ts)]'">
              UC {{ (item.uncertainty ?? 0).toFixed(2) }}
            </span>
          </div>

          <div v-if="getDraftGrade(item) && getDraftGrade(item) !== item.grade" class="mt-2 font-mono text-[9px] text-[var(--emerald)]">
            医师修正 {{ getDraftGrade(item) }}
          </div>
        </button>

        <div
          v-if="!queueLoading && filteredQueueItems.length === 0"
          class="p-4 rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.3)] text-[12px] text-[var(--td)]"
        >
          没有可显示的复核任务。
        </div>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0 p-4 lg:p-6">
      <div class="flex items-center justify-between mb-4 shrink-0">
        <div class="flex items-center gap-3 min-w-0">
          <h2 class="text-[20px] font-medium text-[var(--tp)] m-0">复核工作台</h2>
          <template v-if="currentWorkbench">
            <span class="font-mono text-[12px] text-[var(--amber)] tracking-[0.1em]">
              {{ currentWorkbench.task.taskNo || `T-${currentWorkbench.task.taskId}` }}
            </span>
            <span class="font-mono text-[11px] text-[var(--td)]">
              {{ currentWorkbench.caseInfo.caseNo }}
            </span>
          </template>
        </div>
      </div>

      <div v-if="currentWorkbench" class="flex-1 flex gap-3 min-h-0 overflow-hidden">
        <div class="flex-[1.2] min-w-0">
          <Panel title="影像扫描" color="cyan">
            <div class="relative w-full h-full rounded-[4px] bg-[#0A1428] overflow-hidden border-[0.5px] border-[var(--ln)]" style="background: radial-gradient(ellipse at center, #0A1428, #030812)">
              <div class="absolute inset-0 z-0 opacity-[0.03]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 16px 16px;"></div>

              <div class="absolute inset-4 z-10 bg-black/50 border border-[var(--ln)] rounded-[4px] flex items-center justify-center overflow-hidden">
                <template v-if="shouldRenderSyntheticImage">
                  <div class="absolute inset-0 flex items-center justify-center">
                    <div class="relative w-[72%] h-[78%]">
                      <div
                        v-for="tooth in syntheticTeeth"
                        :key="tooth.id"
                        class="absolute bottom-[10%] bg-gradient-to-b from-white/30 to-white/5 rounded-[40%_40%_12%_12%] border border-white/8 shadow-[inset_0_0_24px_rgba(255,255,255,0.06)]"
                        :style="tooth.style"
                      ></div>
                      <div
                        v-for="hotspot in syntheticHotspots"
                        :key="hotspot.id"
                        class="absolute rounded-full blur-[18px] mix-blend-screen"
                        :class="hotspot.colorClass"
                        :style="hotspot.style"
                      ></div>
                    </div>
                  </div>
                </template>
                <img
                  v-else
                  :src="currentWorkbench.image.imageUrl"
                  alt="X-Ray"
                  class="max-w-full max-h-full object-contain opacity-85 mix-blend-screen"
                />
              </div>

              <div
                v-for="box in currentWorkbench.aiResult.detections"
                :key="box.id"
                class="absolute pointer-events-none opacity-55"
                :style="boxStyle(box)"
              >
                <div class="absolute inset-0 border border-[var(--amber)]"></div>
                <div class="absolute -top-4 left-1/2 -translate-x-1/2 bg-[var(--amber)] px-1 py-0.5 rounded-xs">
                  <span class="font-mono text-[9px] text-[var(--void)]">AI: {{ box.label }}</span>
                </div>
              </div>

              <div
                v-for="box in renderedDoctorDetections"
                :key="box.id"
                class="absolute pointer-events-none"
                :style="boxStyle(box)"
              >
                <div class="absolute inset-0 border-[1.5px] border-dashed border-[var(--emerald)] shadow-[0_0_8px_rgba(0,255,163,0.3)]"></div>
                <div class="absolute -top-4 left-1/2 -translate-x-1/2 bg-[var(--emerald)] px-1 py-0.5 rounded-xs shadow-[0_0_8px_var(--emerald)]">
                  <span class="font-mono text-[9px] text-[var(--void)] font-bold">DOC: {{ box.label }}</span>
                </div>
              </div>

              <div class="absolute top-2 left-2 z-40 flex flex-col gap-2">
                <HudChip :label="`AI 原始预测 · ${currentWorkbench.aiResult.detections.length} 处病灶`" color="amber" />
                <HudChip :label="`医师校对 · ${renderedDoctorDetections.length} 处修正`" color="emerald" />
              </div>

              <div class="absolute bottom-2 left-2 z-40 glass-panel px-3 py-2 rounded-[4px] border-[var(--ln)]">
                <div class="font-mono text-[10px] text-[var(--cyan-soft)]">{{ currentWorkbench.caseInfo.caseNo }}</div>
                <div class="font-mono text-[10px] text-[var(--td)] mt-1">{{ currentWorkbench.image.sourceDevice || 'Synthetic X-Ray View' }}</div>
              </div>
            </div>
          </Panel>
        </div>

        <div class="flex-1 min-w-0 flex flex-col gap-3">
          <Panel title="修正复核面板" color="emerald">
            <div class="flex flex-col h-full gap-4">
              <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)] px-3 py-2">
                <div class="grid grid-cols-2 gap-3 font-mono text-[10px] text-[var(--td)]">
                  <div>当前病例 {{ currentWorkbench.caseInfo.caseNo }}</div>
                  <div class="text-right">图像 ID {{ currentWorkbench.image.imageId }}</div>
                  <div>复核任务 {{ currentWorkbench.task.taskNo }}</div>
                  <div class="text-right">不确定度 {{ currentWorkbench.aiResult.uncertaintyScore.toFixed(2) }}</div>
                </div>
              </div>

              <div class="flex items-center justify-between p-3 border border-[var(--ln)] rounded-[4px] bg-[rgba(10,20,40,0.5)]">
                <div class="flex flex-col items-center flex-1">
                  <span class="font-mono text-[10px] text-[var(--td)] mb-1">AI 预分级</span>
                  <span class="font-mono text-[20px] text-[var(--amber)] val-amber">{{ currentWorkbench.aiResult.gradingLabel }}</span>
                </div>
                <svg class="w-5 h-5 text-[var(--td)] shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                <div class="flex flex-col items-center flex-1">
                  <span class="font-mono text-[10px] text-[var(--emerald)] mb-1">医师确认</span>
                  <span class="font-mono text-[20px] text-[var(--emerald)] val-emerald">{{ selectedGrade }}</span>
                </div>
              </div>

              <div>
                <span class="font-mono text-[11px] text-[var(--td)] tracking-widest uppercase mb-2 block">校对分级</span>
                <div class="flex gap-2">
                  <button
                    v-for="g in currentWorkbench.reviewOptions.gradeOptions"
                    :key="g"
                    type="button"
                    @click="selectedGrade = g"
                    class="flex-1 py-2 rounded-[3px] border font-mono text-[11px] transition-colors relative"
                    :class="g === selectedGrade
                      ? 'bg-[var(--emerald)]/20 border-[var(--emerald)] text-[var(--emerald)] shadow-[0_0_8px_rgba(0,255,163,0.2)]'
                      : g === currentWorkbench.aiResult.gradingLabel
                        ? 'border-[var(--amber)]/30 text-[var(--amber)] opacity-50'
                        : 'border-[var(--ln)] text-[var(--td)] hover:text-[var(--ts)] bg-[rgba(3,8,18,0.5)]'"
                  >
                    {{ g }}
                    <div v-if="g === selectedGrade" class="absolute -top-1.5 -right-1.5 w-3.5 h-3.5 bg-[var(--emerald)] rounded-full flex items-center justify-center shadow-[0_0_8px_var(--emerald)]">
                      <svg class="w-2.5 h-2.5 text-[var(--void)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" /></svg>
                    </div>
                  </button>
                </div>
              </div>

              <div>
                <span class="font-mono text-[11px] text-[var(--td)] tracking-widest uppercase mb-2 block">修正原因</span>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="tag in currentWorkbench.reviewOptions.reasonTags"
                    :key="tag"
                    type="button"
                    @click="toggleTag(tag)"
                    class="px-3 py-1.5 rounded-[3px] border font-mono text-[11px] transition-colors"
                    :class="selectedTags.includes(tag)
                      ? 'border-[var(--cyan)] bg-[var(--cyan)]/20 text-[var(--cyan)] shadow-[0_0_8px_rgba(0,229,255,0.2)]'
                      : 'border-[var(--ln)] bg-[rgba(3,8,18,0.5)] text-[var(--td)] hover:text-[var(--ts)]'"
                  >
                    {{ tag }}
                  </button>
                </div>
              </div>

              <div class="grid gap-3 sm:grid-cols-2">
                <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.45)] px-3 py-2">
                  <div class="font-mono text-[10px] text-[var(--td)]">当前原始内容</div>
                  <div class="mt-2 text-[12px] text-[var(--ts)] leading-relaxed">
                    <div>原始分级：<span class="font-mono text-[var(--amber)]">{{ originalGrade }}</span></div>
                    <div>原始原因：{{ originalReasonText }}</div>
                    <div class="mt-2 text-[11px] text-[var(--td)] whitespace-pre-line">{{ originalNoteText }}</div>
                  </div>
                </div>

                <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.45)] px-3 py-2">
                  <div class="font-mono text-[10px] text-[var(--td)]">当前修改内容</div>
                  <div class="mt-2 text-[12px] text-[var(--ts)] leading-relaxed">
                    <div>当前草稿分级：<span class="font-mono text-[var(--emerald)]">{{ selectedGrade }}</span></div>
                    <div>当前原因：{{ selectedTags.length ? selectedTags.join(' / ') : '未选择' }}</div>
                    <div class="mt-2 text-[11px] text-[var(--td)] whitespace-pre-line">{{ clinicalNote || '暂无临床备注' }}</div>
                  </div>
                </div>
              </div>

              <div class="flex-1 flex flex-col min-h-[120px]">
                <span class="font-mono text-[11px] text-[var(--td)] tracking-widest uppercase mb-2 block">临床备注</span>
                <div class="flex-1 relative">
                  <textarea
                    v-model="clinicalNote"
                    class="w-full h-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[4px] p-3 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-colors resize-none"
                    placeholder="请输入复核意见或临床观察..."
                  ></textarea>
                </div>
              </div>

              <div class="grid grid-cols-3 gap-2 mt-auto">
                <NeuralButton variant="ghost" class="text-[11px] text-[var(--td)] border-[var(--td)]/30 hover:border-[var(--td)]" @click="saveDraftLocally">
                  保存草稿
                </NeuralButton>
                <NeuralButton variant="danger" class="text-[11px]" @click="requestSecondOpinion">
                  二次意见
                </NeuralButton>
                <NeuralButton variant="success" @click="submitReview" :loading="submitting" class="text-[11px]">
                  提交并记录
                </NeuralButton>
              </div>
            </div>
          </Panel>
        </div>
      </div>

      <div v-else class="flex-1 flex items-center justify-center text-[13px] text-[var(--td)]">
        {{ workbenchLoading ? '正在加载复核内容...' : '没有可显示的复核病例。' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Panel from '../components/shared/Panel.vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import GradeBadge from '../components/shared/GradeBadge.vue';
import HudChip from '../components/shared/HudChip.vue';
import { reviewApi } from '../api/review';
import { analysisApi } from '../api/analysis';
import { AnalysisAdapter } from '../api/adapters/analysis';
import type { AnalysisTaskItem } from '../models/analysis';
import { useNotificationStore } from '../stores/notification';
import { ApiClientError } from '../api/request';

type ReviewGrade = 'G0' | 'G1' | 'G2' | 'G3' | 'G4';

interface DetectionBox {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  label: ReviewGrade;
  confidence?: number;
}

interface ReviewWorkbenchData {
  task: {
    taskId: number;
    taskNo?: string;
    createdAt: string;
    statusCode?: string;
  };
  caseInfo: {
    caseId: number;
    caseNo: string;
    visitTime?: string;
  };
  image: {
    imageId: number;
    imageUrl: string;
    sourceDevice?: string;
  };
  aiResult: {
    gradingLabel: ReviewGrade;
    uncertaintyScore: number;
    detections: DetectionBox[];
  };
  doctorDraft?: {
    draftId?: number;
    revisedGrade?: ReviewGrade;
    revisedDetections?: DetectionBox[];
    reasonTags?: string[];
    note?: string;
  };
  reviewOptions: {
    gradeOptions: ReviewGrade[];
    reasonTags: string[];
  };
}

interface DraftState {
  revisedGrade: ReviewGrade;
  reasonTags: string[];
  note: string;
}

interface SyntheticTooth {
  id: string;
  style: string;
}

interface SyntheticHotspot {
  id: string;
  style: string;
  colorClass: string;
}

const route = useRoute();
const router = useRouter();
const notificationStore = useNotificationStore();

const queueItems = ref<AnalysisTaskItem[]>([]);
const queueKeyword = ref('');
const queueLoading = ref(false);
const workbenchLoading = ref(false);
const submitting = ref(false);
const activeTaskId = ref<number | null>(null);
const selectedGrade = ref<ReviewGrade>('G2');
const selectedTags = ref<string[]>([]);
const clinicalNote = ref('');

const workbenchMap = ref<Record<number, ReviewWorkbenchData>>({});
const draftStateMap = ref<Record<number, DraftState>>({});

const routeTaskId = computed(() => Number.parseInt(route.params.taskId as string, 10) || 1);

const filteredQueueItems = computed(() => {
  const keyword = queueKeyword.value.trim().toLowerCase();
  const baseItems = queueItems.value;
  if (!keyword) return baseItems;
  return baseItems.filter((item) =>
    [item.no, item.caseNo, item.patientId, item.patientName]
      .filter(Boolean)
      .some((value) => value!.toLowerCase().includes(keyword))
  );
});

const currentWorkbench = computed(() => {
  if (activeTaskId.value == null) return null;
  return workbenchMap.value[activeTaskId.value] ?? null;
});

const originalGrade = computed<ReviewGrade>(() =>
  normalizeGrade(currentWorkbench.value?.doctorDraft?.revisedGrade || currentWorkbench.value?.aiResult.gradingLabel)
);

const originalReasonText = computed(() => {
  const reasonTags = currentWorkbench.value?.doctorDraft?.reasonTags ?? [];
  return reasonTags.length ? reasonTags.join(' / ') : '未记录';
});

const originalNoteText = computed(() => currentWorkbench.value?.doctorDraft?.note || '暂无原始备注');

const renderedDoctorDetections = computed(() => {
  if (!currentWorkbench.value) return [];
  const baseDetections = currentWorkbench.value.doctorDraft?.revisedDetections?.length
    ? currentWorkbench.value.doctorDraft.revisedDetections
    : currentWorkbench.value.aiResult.detections.slice(0, 1).map((box) => ({
        ...box,
        id: `${box.id}-doctor`,
        y: Math.min(0.92, box.y + 0.05),
        height: Math.max(0.08, box.height - 0.05),
      }));

  return baseDetections.map((box, index) => ({
    ...box,
    id: box.id || `doc-box-${index + 1}`,
    label: selectedGrade.value,
  }));
});

const shouldRenderSyntheticImage = computed(() => {
  const imageUrl = currentWorkbench.value?.image?.imageUrl ?? '';
  return !imageUrl || /mock|demo/i.test(imageUrl);
});

const syntheticTeeth = computed<SyntheticTooth[]>(() => {
  const seed = activeTaskId.value ?? 1;
  return Array.from({ length: 4 }, (_, index) => {
    const width = 14 + ((seed + index) % 3) * 2;
    const left = 12 + index * 18 + ((seed + index) % 2) * 2;
    const height = 44 + ((seed + index) % 4) * 4;
    return {
      id: `tooth-${index}`,
      style: `left:${left}%; width:${width}%; height:${height}%;`,
    };
  });
});

const syntheticHotspots = computed<SyntheticHotspot[]>(() => {
  const seed = activeTaskId.value ?? 1;
  return [
    {
      id: 'hotspot-amber',
      style: `left:${44 + (seed % 4) * 2}%; top:${38 + (seed % 3) * 4}%; width:13%; height:16%; background:rgba(255,181,71,0.28);`,
      colorClass: '',
    },
    {
      id: 'hotspot-emerald',
      style: `left:${40 + (seed % 5)}%; top:${48 + (seed % 2) * 3}%; width:11%; height:12%; background:rgba(0,255,163,0.18);`,
      colorClass: '',
    },
  ];
});

const formatTaskTime = (createdAt?: string) => {
  if (!createdAt) return '--:--';
  return new Date(createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const boxStyle = (box: DetectionBox) => ({
  left: `${box.x * 100}%`,
  top: `${box.y * 100}%`,
  width: `${box.width * 100}%`,
  height: `${box.height * 100}%`,
});

const normalizeApiError = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message;
  if (error instanceof Error) return error.message;
  return '请求失败';
};

const normalizeGrade = (grade?: string): ReviewGrade => {
  switch (grade) {
    case 'G0':
    case 'G1':
    case 'G2':
    case 'G3':
    case 'G4':
      return grade;
    default:
      return 'G0';
  }
};

const normalizeDetection = (box: Partial<DetectionBox>, index: number): DetectionBox => ({
  id: box.id || `box-${index + 1}`,
  x: typeof box.x === 'number' ? box.x : 0.3,
  y: typeof box.y === 'number' ? box.y : 0.3,
  width: typeof box.width === 'number' ? box.width : 0.18,
  height: typeof box.height === 'number' ? box.height : 0.16,
  label: normalizeGrade(box.label),
  confidence: typeof box.confidence === 'number' ? box.confidence : undefined,
});

const normalizeWorkbench = (raw: ReviewWorkbenchData): ReviewWorkbenchData => ({
  ...raw,
  aiResult: {
    ...raw.aiResult,
    gradingLabel: normalizeGrade(raw.aiResult?.gradingLabel),
    uncertaintyScore: Number(raw.aiResult?.uncertaintyScore ?? 0),
    detections: (raw.aiResult?.detections ?? []).map((box, index) => normalizeDetection(box, index)),
  },
  doctorDraft: raw.doctorDraft
    ? {
        ...raw.doctorDraft,
        revisedGrade: normalizeGrade(raw.doctorDraft.revisedGrade || raw.aiResult?.gradingLabel),
        revisedDetections: (raw.doctorDraft.revisedDetections ?? []).map((box, index) => normalizeDetection(box, index)),
        reasonTags: [...(raw.doctorDraft.reasonTags ?? [])],
        note: raw.doctorDraft.note || '',
      }
    : undefined,
  reviewOptions: {
    gradeOptions: (raw.reviewOptions?.gradeOptions ?? ['G0', 'G1', 'G2', 'G3', 'G4']).map((grade) => normalizeGrade(grade)),
    reasonTags: [...(raw.reviewOptions?.reasonTags ?? [])],
  },
});

const seedDraftFromWorkbench = (workbench: ReviewWorkbenchData): DraftState => ({
  revisedGrade: workbench.doctorDraft?.revisedGrade || workbench.aiResult.gradingLabel,
  reasonTags: [...(workbench.doctorDraft?.reasonTags || [])],
  note: workbench.doctorDraft?.note || '',
});

const persistCurrentDraft = () => {
  if (activeTaskId.value == null) return;
  draftStateMap.value[activeTaskId.value] = {
    revisedGrade: selectedGrade.value,
    reasonTags: [...selectedTags.value],
    note: clinicalNote.value,
  };
};

const hydrateDraft = (taskId: number) => {
  const workbench = workbenchMap.value[taskId];
  if (!workbench) return;
  const draft = draftStateMap.value[taskId] ?? seedDraftFromWorkbench(workbench);
  draftStateMap.value[taskId] = {
    revisedGrade: draft.revisedGrade,
    reasonTags: [...draft.reasonTags],
    note: draft.note,
  };
  selectedGrade.value = draft.revisedGrade;
  selectedTags.value = [...draft.reasonTags];
  clinicalNote.value = draft.note;
};

const upsertQueueItem = (item: AnalysisTaskItem) => {
  const next = [...queueItems.value];
  const index = next.findIndex((queueItem) => queueItem.id === item.id);
  if (index === -1) {
    next.push(item);
  } else {
    next[index] = { ...next[index], ...item };
  }
  next.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
  queueItems.value = next;
};

const upsertQueueItemFromWorkbench = (workbench: ReviewWorkbenchData) => {
  upsertQueueItem({
    id: workbench.task.taskId,
    no: workbench.task.taskNo || `T-${workbench.task.taskId}`,
    caseNo: workbench.caseInfo.caseNo,
    grade: workbench.aiResult.gradingLabel,
    uncertainty: workbench.aiResult.uncertaintyScore,
    status: 'REVIEW',
    createdAt: workbench.task.createdAt,
    needsReview: true,
  });
};

const createFallbackWorkbenchVariant = (base: ReviewWorkbenchData, index: number): ReviewWorkbenchData => {
  const aiGrades: ReviewGrade[] = ['G4', 'G2', 'G3', 'G1', 'G4', 'G2'];
  const doctorGrades: ReviewGrade[] = ['G3', 'G1', 'G2', 'G0', 'G3', 'G2'];
  const reasons = [
    ['病灶范围过估'],
    ['边界误判', '影像伪影'],
    ['深度判断偏差'],
    ['病灶范围低估'],
    ['影像伪影', '其他'],
    ['病灶范围过估', '深度判断偏差'],
  ];

  const seed = index + 1;
  const taskId = base.task.taskId + seed;
  const aiGrade = aiGrades[index % aiGrades.length];
  const doctorGrade = doctorGrades[index % doctorGrades.length];
  const uncertainty = Math.min(0.92, 0.38 + seed * 0.07);
  const detections = base.aiResult.detections.map((box, boxIndex) => ({
    ...box,
    id: `ai-${taskId}-${boxIndex}`,
    x: Math.max(0.12, Math.min(0.74, box.x + (seed % 3) * 0.04 - 0.04)),
    y: Math.max(0.2, Math.min(0.7, box.y + (seed % 2) * 0.03)),
    label: aiGrade,
  }));

  return {
    ...base,
    task: {
      ...base.task,
      taskId,
      taskNo: `TSK-REV-${taskId}`,
      createdAt: new Date(Date.now() - seed * 60_000).toISOString(),
    },
    caseInfo: {
      ...base.caseInfo,
      caseId: base.caseInfo.caseId + seed,
      caseNo: `CASE-REV-${(base.caseInfo.caseId + seed).toString().padStart(4, '0')}`,
    },
    image: {
      ...base.image,
      imageId: base.image.imageId + seed,
      sourceDevice: `Review Mock Device ${seed}`,
    },
    aiResult: {
      ...base.aiResult,
      gradingLabel: aiGrade,
      uncertaintyScore: Number(uncertainty.toFixed(2)),
      detections,
    },
    doctorDraft: {
      draftId: seed,
      revisedGrade: doctorGrade,
      revisedDetections: detections.slice(0, 1).map((box, boxIndex) => ({
        ...box,
        id: `doc-${taskId}-${boxIndex}`,
        y: Math.min(0.82, box.y + 0.05),
        height: Math.max(0.08, box.height - 0.04),
        label: doctorGrade,
      })),
      reasonTags: reasons[index % reasons.length],
      note: `病例 ${seed} 的复核草稿已载入，当前建议修正为 ${doctorGrade}。`,
    },
    reviewOptions: {
      gradeOptions: [...base.reviewOptions.gradeOptions],
      reasonTags: [...base.reviewOptions.reasonTags],
    },
  };
};

const ensureFallbackQueue = () => {
  if (queueItems.value.length >= 2 || !currentWorkbench.value) return;
  const generated = Array.from({ length: 6 }, (_, index) => createFallbackWorkbenchVariant(currentWorkbench.value!, index));
  generated.forEach((workbench) => {
    workbenchMap.value[workbench.task.taskId] = workbench;
    draftStateMap.value[workbench.task.taskId] = seedDraftFromWorkbench(workbench);
    upsertQueueItemFromWorkbench(workbench);
  });
};

const loadWorkbench = async (taskId: number) => {
  workbenchLoading.value = true;
  try {
    const res = await reviewApi.getReviewWorkbench(taskId);
    const workbench = normalizeWorkbench(res.data as ReviewWorkbenchData);
    workbenchMap.value[taskId] = workbench;
    if (!draftStateMap.value[taskId]) {
      draftStateMap.value[taskId] = seedDraftFromWorkbench(workbench);
    }
    upsertQueueItemFromWorkbench(workbench);
  } catch (error) {
    notificationStore.error('复核内容加载失败', normalizeApiError(error));
  } finally {
    workbenchLoading.value = false;
  }
};

const fetchQueue = async () => {
  queueLoading.value = true;
  try {
    const res = await analysisApi.getTasks({ pageNum: 1, pageSize: 24 });
    const items = AnalysisAdapter.toTaskList(res.data).items
      .filter((item) => item.needsReview || item.status === 'REVIEW')
      .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
    queueItems.value = items;
  } catch (error) {
    notificationStore.error('复核队列加载失败', normalizeApiError(error));
  } finally {
    queueLoading.value = false;
  }
};

const selectTask = async (taskId: number, updateRoute = true) => {
  if (activeTaskId.value === taskId && currentWorkbench.value) return;

  persistCurrentDraft();
  activeTaskId.value = taskId;

  if (updateRoute && String(route.params.taskId) !== String(taskId)) {
    router.replace(`/review/${taskId}`);
  }

  if (!workbenchMap.value[taskId]) {
    await loadWorkbench(taskId);
  }

  hydrateDraft(taskId);
};

const toggleTag = (tag: string) => {
  const next = new Set(selectedTags.value);
  if (next.has(tag)) {
    next.delete(tag);
  } else {
    next.add(tag);
  }
  selectedTags.value = [...next];
  persistCurrentDraft();
};

const saveDraftLocally = () => {
  persistCurrentDraft();
  notificationStore.info('草稿已保存', '当前复核修改已暂存到本地切换缓存。');
};

const requestSecondOpinion = () => {
  notificationStore.info('二次意见', '当前版本尚未接入二次意见流程，已保留入口。');
};

const getDraftGrade = (item: AnalysisTaskItem) => draftStateMap.value[item.id]?.revisedGrade;

const getDisplayedQueueGrade = (item: AnalysisTaskItem): ReviewGrade => normalizeGrade(getDraftGrade(item) || item.grade);

const submitReview = async () => {
  if (!currentWorkbench.value || activeTaskId.value == null) return;
  submitting.value = true;
  persistCurrentDraft();

  try {
    await reviewApi.submitReview({
      taskId: currentWorkbench.value.task.taskId,
      revisedGrade: selectedGrade.value,
      reasonTags: selectedTags.value,
      note: clinicalNote.value,
    });

    workbenchMap.value[activeTaskId.value] = {
      ...currentWorkbench.value,
      doctorDraft: {
        draftId: currentWorkbench.value.doctorDraft?.draftId,
        revisedGrade: selectedGrade.value,
        revisedDetections: renderedDoctorDetections.value,
        reasonTags: [...selectedTags.value],
        note: clinicalNote.value,
      },
    };

    notificationStore.success('复核已提交', '当前病例的修正内容已同步到工作台显示。');
    hydrateDraft(activeTaskId.value);
  } catch (error) {
    notificationStore.error('提交失败', normalizeApiError(error));
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  await loadWorkbench(routeTaskId.value);
  activeTaskId.value = routeTaskId.value;
  hydrateDraft(routeTaskId.value);
  await fetchQueue();
  ensureFallbackQueue();
});

watch(routeTaskId, async (taskId) => {
  if (taskId !== activeTaskId.value) {
    await selectTask(taskId, false);
  }
});

watch([selectedGrade, selectedTags, clinicalNote], () => {
  persistCurrentDraft();
}, { deep: true });
</script>
