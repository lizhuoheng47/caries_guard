<template>
  <div v-if="detail" class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <div class="flex items-center justify-between mb-4 shrink-0 gap-4">
      <div class="flex items-center gap-3 min-w-0">
        <NeuralButton variant="ghost" @click="router.back()">返回</NeuralButton>
        <div class="h-4 w-px bg-[var(--ln)]"></div>
        <div class="min-w-0">
          <div class="font-mono text-[11px] text-[var(--td)] tracking-[0.12em] uppercase">
            Analysis / {{ detail.task.no }}
          </div>
          <div class="flex items-center gap-2 mt-1">
            <h2 class="text-[20px] font-medium text-[var(--tp)] truncate">龋病影像分析详情</h2>
            <StatusChip :status="taskStatus" />
          </div>
        </div>
      </div>

      <div class="font-mono text-[11px] text-[var(--td)] text-right">
        <div>{{ detail.caseInfo.no || 'CASE-UNKNOWN' }}</div>
        <div v-if="detail.task.inferenceMillis">耗时 {{ (detail.task.inferenceMillis / 1000).toFixed(2) }}s</div>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4 shrink-0">
      <KpiCard label="AI 分级" :value="detail.summary.grade" color="cyan" />
      <KpiCard label="病灶数量" :value="String(detail.summary.lesionCount)" color="amber" />
      <KpiCard label="置信度" :value="detail.summary.confidence != null ? `${Math.round(detail.summary.confidence * 100)}%` : '--'" color="emerald" />
      <KpiCard label="风险等级" :value="detail.summary.riskLevel || '--'" :color="detail.summary.riskLevel?.toUpperCase().includes('HIGH') ? 'magenta' : 'violet'" />
    </div>

    <div class="flex-1 min-h-0 grid grid-cols-1 xl:grid-cols-[1.35fr_0.95fr] gap-3 mb-4 overflow-hidden">
      <Panel title="影像与标注" color="cyan">
        <template #meta>
          <div class="flex gap-1 bg-[rgba(3,8,18,0.55)] p-0.5 rounded-[3px] border border-[var(--ln)]">
            <button
              v-for="option in canvasOptions"
              :key="option.code"
              type="button"
              class="px-2 py-0.5 text-[10px] font-mono rounded-[2px] transition-colors"
              :class="activeCanvas === option.code ? 'bg-[var(--cyan)]/20 text-[var(--cyan)]' : 'text-[var(--td)] hover:text-[var(--tp)]'"
              @click="activeCanvas = option.code"
            >
              {{ option.label }}
            </button>
          </div>
        </template>

        <div class="h-full flex flex-col gap-3">
          <div class="relative flex-1 min-h-[360px] rounded-[4px] border border-[var(--ln)] bg-[rgba(3,8,18,0.68)] overflow-hidden">
            <div class="absolute inset-0 opacity-[0.05]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 18px 18px;"></div>

            <div class="absolute inset-3 rounded-[4px] overflow-hidden border border-[var(--ln)] bg-black/30 flex items-center justify-center">
              <img
                v-if="currentCanvasUrl"
                :src="currentCanvasUrl"
                alt="analysis canvas"
                class="max-w-full max-h-full object-contain"
              />
              <div v-else class="text-[12px] text-[var(--td)]">当前没有可显示的图像资源</div>
            </div>

            <div
              v-if="shouldDrawBoxes"
              v-for="lesion in drawableLesions"
              :key="lesion.id"
              class="absolute pointer-events-none"
              :style="lesionBoxStyle(lesion)"
            >
              <div
                class="absolute inset-0 border"
                :class="lesionTone(lesion.severityCode).border"
              ></div>
              <div
                class="absolute -top-5 left-0 px-1.5 py-0.5 rounded-[2px] font-mono text-[9px] text-[var(--void)] whitespace-nowrap"
                :class="lesionTone(lesion.severityCode).badge"
              >
                {{ lesion.severityCode || 'LESION' }} {{ lesion.toothCode ? `T${lesion.toothCode}` : '' }}
              </div>
            </div>

            <div class="absolute top-3 left-3 z-10 flex flex-col gap-2">
              <div class="px-2 py-1 rounded-[3px] border border-[var(--ln)] bg-[rgba(3,8,18,0.7)] font-mono text-[10px] text-[var(--cyan)]">
                {{ activeCanvasLabel }}
              </div>
              <div class="px-2 py-1 rounded-[3px] border border-[var(--ln)] bg-[rgba(3,8,18,0.7)] font-mono text-[10px] text-[var(--ts)]">
                {{ detail.summary.lesionCount }} 处病灶 / {{ detail.summary.abnormalToothCount }} 颗异常牙
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
            <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)] p-3">
              <div class="font-mono text-[10px] text-[var(--td)] uppercase tracking-[0.12em] mb-2">临床摘要</div>
              <p class="text-[13px] text-[var(--tp)] leading-relaxed">
                {{ detail.summary.clinicalSummary || '暂无临床摘要。' }}
              </p>
            </div>
            <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)] p-3">
              <div class="font-mono text-[10px] text-[var(--td)] uppercase tracking-[0.12em] mb-2">患者信息</div>
              <div class="text-[12px] text-[var(--ts)] flex flex-col gap-1">
                <span>患者标识：{{ detail.patient.idMasked || '--' }}</span>
                <span>性别 / 年龄：{{ detail.patient.gender || '--' }} / {{ detail.patient.age ?? '--' }}</span>
                <span>设备：{{ detail.image.sourceDevice || '--' }}</span>
              </div>
            </div>
            <div class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)] p-3">
              <div class="font-mono text-[10px] text-[var(--td)] uppercase tracking-[0.12em] mb-2">知识库版本</div>
              <div class="text-[13px] text-[var(--tp)]">{{ detail.summary.knowledgeVersion || '未返回' }}</div>
              <div class="mt-2 text-[11px] text-[var(--td)]">
                {{ detail.rag.enabled ? '已启用本地知识库增强' : '未启用知识库增强' }}
              </div>
            </div>
          </div>
        </div>
      </Panel>

      <div class="min-h-0 flex flex-col gap-3 overflow-hidden">
        <Panel title="病灶列表" color="amber">
          <div class="flex flex-col gap-3">
            <div
              v-for="lesion in detail.summary.lesions"
              :key="lesion.id"
              class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.5)] p-3"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-[12px] text-[var(--tp)]">{{ lesion.severityCode || 'UNKNOWN' }}</span>
                    <span class="font-mono text-[11px] text-[var(--td)]">{{ lesion.toothCode ? `牙位 ${lesion.toothCode}` : '未定位牙位' }}</span>
                  </div>
                  <div class="mt-2 text-[12px] text-[var(--ts)] leading-relaxed">{{ lesion.summary || '暂无病灶描述。' }}</div>
                </div>
                <div class="text-right font-mono text-[11px] text-[var(--td)] shrink-0">
                  <div>{{ lesion.areaRatio != null ? `${(lesion.areaRatio * 100).toFixed(2)}%` : '--' }}</div>
                  <div>{{ lesion.areaPx != null ? `${lesion.areaPx}px` : '--' }}</div>
                </div>
              </div>
              <div class="mt-3 text-[11px] text-[var(--td)]">
                治疗建议：{{ lesion.treatmentSuggestion || '请结合医生复核。' }}
              </div>
            </div>

            <div v-if="detail.summary.lesions.length === 0" class="text-[12px] text-[var(--td)]">
              当前结果未返回结构化病灶列表。
            </div>
          </div>
        </Panel>

        <Panel title="治疗与建议" color="emerald">
          <div class="flex flex-col gap-3">
            <div class="rounded-[4px] border border-[var(--emerald)]/25 bg-[var(--emerald)]/5 p-3">
              <div class="font-mono text-[10px] text-[var(--emerald)] uppercase tracking-[0.12em] mb-2">知识库增强建议</div>
              <div class="text-[13px] text-[var(--tp)] leading-relaxed">
                {{ detail.summary.followUpRecommendation || detail.rag.answer || '暂无建议。' }}
              </div>
            </div>

            <div>
              <div class="font-mono text-[10px] text-[var(--td)] uppercase tracking-[0.12em] mb-2">治疗计划</div>
              <div class="flex flex-col gap-2">
                <div
                  v-for="(item, index) in detail.summary.treatmentPlan"
                  :key="`${item.title}-${index}`"
                  class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.45)] p-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="text-[13px] text-[var(--tp)]">{{ item.title }}</span>
                    <span class="font-mono text-[10px] text-[var(--emerald)]">{{ item.priority }}</span>
                  </div>
                  <div class="mt-2 text-[12px] text-[var(--ts)] leading-relaxed">{{ item.details }}</div>
                </div>
                <div v-if="detail.summary.treatmentPlan.length === 0" class="text-[12px] text-[var(--td)]">
                  当前结果未返回结构化治疗计划。
                </div>
              </div>
            </div>
          </div>
        </Panel>

        <Panel title="知识库引用" color="violet">
          <div class="flex flex-col gap-2">
            <div
              v-for="citation in detail.summary.citations"
              :key="citation.index"
              class="rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.45)] p-3"
            >
              <div class="flex items-center justify-between gap-3">
                <span class="text-[13px] text-[var(--tp)]">{{ citation.title }}</span>
                <span class="font-mono text-[10px] text-[var(--violet)]">#{{ citation.index }}</span>
              </div>
              <div class="mt-2 text-[12px] text-[var(--ts)] leading-relaxed">{{ citation.excerpt || '无摘要片段。' }}</div>
              <div v-if="citation.sourceUri" class="mt-2 font-mono text-[10px] text-[var(--td)]">{{ citation.sourceUri }}</div>
            </div>
            <div v-if="detail.summary.citations.length === 0" class="text-[12px] text-[var(--td)]">
              当前结果未返回知识库引用。
            </div>
          </div>
        </Panel>
      </div>
    </div>

    <div class="shrink-0 mb-4">
      <Panel title="处理时间线" color="violet">
        <div class="flex flex-wrap gap-3">
          <div
            v-for="node in detail.timeline"
            :key="node.code"
            class="min-w-[180px] rounded-[4px] border border-[var(--ln)] bg-[rgba(10,20,40,0.45)] p-3"
          >
            <div class="flex items-center justify-between gap-3">
              <span class="text-[13px] text-[var(--tp)]">{{ node.name }}</span>
              <span class="font-mono text-[10px]" :class="timelineTone(node.status)">{{ node.status }}</span>
            </div>
            <div v-if="node.description" class="mt-2 text-[12px] text-[var(--ts)]">{{ node.description }}</div>
            <div v-if="node.time" class="mt-2 font-mono text-[10px] text-[var(--td)]">{{ formatDateTime(node.time) }}</div>
          </div>
          <div v-if="detail.timeline.length === 0" class="text-[12px] text-[var(--td)]">当前没有可展示的时间线节点。</div>
        </div>
      </Panel>
    </div>
  </div>

  <div v-else-if="store.loading" class="flex-1 flex flex-col items-center justify-center">
    <div class="w-16 h-16 rounded-full border border-[var(--cyan)]/30 border-t-[var(--cyan)] animate-spin mb-4 shadow-[0_0_15px_rgba(0,229,255,0.2)]"></div>
    <div class="font-mono text-[12px] text-[var(--cyan)] tracking-[0.2em] animate-pulse-opacity">LOADING ANALYSIS...</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { AnalysisLesion } from '../models/analysis';
import { useAnalysisStore } from '../stores/analysis';
import KpiCard from '../components/shared/KpiCard.vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import Panel from '../components/shared/Panel.vue';
import StatusChip from '../components/shared/StatusChip.vue';

const route = useRoute();
const router = useRouter();
const store = useAnalysisStore();

const activeCanvas = ref('source');

const detail = computed(() => store.currentDetail);

const taskStatus = computed<'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED'>(() => {
  const status = (detail.value?.task.status || '').toUpperCase();
  if (status === 'SUCCESS' || status === 'DONE') return 'DONE';
  if (status === 'RUNNING') return 'RUNNING';
  if (status === 'REVIEW') return 'REVIEW';
  if (status === 'FAILED') return 'FAILED';
  return 'QUEUED';
});

const canvasOptions = computed(() => {
  const options = [
    { code: 'source', label: '原图', url: detail.value?.image.url },
  ];

  for (const asset of detail.value?.task.visualAssets || []) {
    if (!asset.url) continue;
    options.push({
      code: asset.type.toLowerCase(),
      label: asset.label,
      url: asset.url,
    });
  }
  return options;
});

const activeCanvasLabel = computed(() => canvasOptions.value.find((item) => item.code === activeCanvas.value)?.label || '原图');

const currentCanvasUrl = computed(() => canvasOptions.value.find((item) => item.code === activeCanvas.value)?.url);

const drawableLesions = computed(() =>
  (detail.value?.summary.lesions || []).filter((lesion) => Array.isArray(lesion.bbox) && lesion.bbox.length === 4),
);

const shouldDrawBoxes = computed(() =>
  activeCanvas.value === 'source' &&
  Boolean(detail.value?.summary.annotationImageWidth) &&
  Boolean(detail.value?.summary.annotationImageHeight) &&
  drawableLesions.value.length > 0,
);

const lesionTone = (severityCode?: string) => {
  switch ((severityCode || '').toUpperCase()) {
    case 'C3':
      return {
        border: 'border-[var(--magenta)] shadow-[0_0_12px_rgba(255,79,129,0.28)]',
        badge: 'bg-[var(--magenta)]',
      };
    case 'C2':
      return {
        border: 'border-[var(--amber)] shadow-[0_0_12px_rgba(255,181,71,0.28)]',
        badge: 'bg-[var(--amber)]',
      };
    default:
      return {
        border: 'border-[var(--cyan)] shadow-[0_0_12px_rgba(0,229,255,0.28)]',
        badge: 'bg-[var(--cyan)]',
      };
  }
};

const lesionBoxStyle = (lesion: AnalysisLesion) => {
  const bbox = lesion.bbox || [0, 0, 0, 0];
  const imageWidth = detail.value?.summary.annotationImageWidth || 1;
  const imageHeight = detail.value?.summary.annotationImageHeight || 1;
  return {
    left: `${(bbox[0] / imageWidth) * 100}%`,
    top: `${(bbox[1] / imageHeight) * 100}%`,
    width: `${((bbox[2] - bbox[0]) / imageWidth) * 100}%`,
    height: `${((bbox[3] - bbox[1]) / imageHeight) * 100}%`,
  };
};

const timelineTone = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'text-[var(--emerald)]';
    case 'CURRENT':
      return 'text-[var(--cyan)]';
    default:
      return 'text-[var(--td)]';
  }
};

const formatDateTime = (value: string) => new Date(value).toLocaleString();

const fetchDetail = async () => {
  const taskIdentifier = String(route.params.taskId || '');
  if (!taskIdentifier) return;
  await store.fetchDetail(taskIdentifier);
};

watch(detail, () => {
  activeCanvas.value = 'source';
});

watch(
  () => route.params.taskId,
  async (taskId, previous) => {
    if (taskId && taskId !== previous) {
      await fetchDetail();
    }
  },
);

onMounted(fetchDetail);
</script>
