<template>
  <div v-if="detail" class="page">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">Analysis Detail</div>
        <h1 class="page-hello-title">影像分析详情</h1>
      </div>
      <div style="display: flex; align-items: center; gap: 10px">
        <button class="btn btn-primary" @click="openDoctorQa">
          <AppIcon name="sparkle" :size="14" />
          问答解释
        </button>
        <button class="btn btn-ghost" @click="router.back()">
          <AppIcon name="chevron_right" :size="14" style="transform: rotate(180deg)" />
          返回
        </button>
        <span class="chip" :style="statusChipStyle(taskStatus)">{{ taskStatusLabel(taskStatus) }}</span>
      </div>
    </div>

    <div class="card" style="padding: 18px; margin-bottom: 16px">
      <div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px">
        <div class="home-stat" style="padding: 0">
          <div class="micro">AI 分级</div>
          <div class="home-stat-n" style="font-size: 24px; margin-top: 8px">{{ detail.summary.grade }}</div>
        </div>
        <div class="home-stat" style="padding: 0">
          <div class="micro">病灶数量</div>
          <div class="home-stat-n" style="font-size: 24px; margin-top: 8px">{{ detail.summary.lesionCount }}</div>
        </div>
        <div class="home-stat" style="padding: 0">
          <div class="micro">置信度</div>
          <div class="home-stat-n" style="font-size: 24px; margin-top: 8px">
            {{ detail.summary.confidence != null ? `${Math.round(detail.summary.confidence * 100)}%` : '--' }}
          </div>
        </div>
        <div class="home-stat" style="padding: 0">
          <div class="micro">不确定性</div>
          <div class="home-stat-n" style="font-size: 24px; margin-top: 8px">
            {{ detail.summary.uncertainty != null ? detail.summary.uncertainty.toFixed(2) : '--' }}
          </div>
        </div>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(320px, .95fr); gap: 16px; margin-bottom: 16px">
      <section class="card" style="overflow: hidden">
        <div class="card-head">
          <h3>影像与标注</h3>
          <div class="card-head-actions" style="gap: 8px">
            <button
              v-for="option in canvasOptions"
              :key="option.code"
              class="lib-filter"
              :class="{ on: activeCanvas === option.code }"
              @click="activeCanvas = option.code"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div style="padding: 18px">
          <div style="position: relative; min-height: 460px; border-radius: 18px; overflow: hidden; background: linear-gradient(180deg, #0d1215, #1d2a31); border: 1px solid #23343d">
            <div style="position: absolute; inset: 16px; border-radius: 14px; overflow: hidden; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.24)">
              <img
                v-if="currentCanvasUrl"
                :src="currentCanvasUrl"
                alt="analysis canvas"
                style="max-width: 100%; max-height: 100%; object-fit: contain"
              />
              <div v-else style="color: rgba(255,255,255,.72); font-size: 13px">当前没有可展示的图像资源</div>
            </div>

            <div
              v-for="lesion in drawableLesions"
              v-if="shouldDrawBoxes"
              :key="lesion.id"
              :style="lesionBoxStyle(lesion)"
              style="position: absolute; pointer-events: none"
            >
              <div :style="lesionBorderStyle(lesion.severityCode)"></div>
              <div :style="lesionBadgeStyle(lesion.severityCode)">
                {{ lesion.severityCode || 'LESION' }} {{ lesion.toothCode ? `T${lesion.toothCode}` : '' }}
              </div>
            </div>

            <div style="position: absolute; top: 18px; left: 18px; display: grid; gap: 8px">
              <span class="chip chip-neutral">{{ activeCanvasLabel }}</span>
              <span class="chip chip-neutral">{{ detail.summary.lesionCount }} 个病灶 / {{ detail.summary.abnormalToothCount }} 颗异常牙</span>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px">
            <div class="card" style="padding: 14px">
              <div class="micro">临床摘要</div>
              <p style="font-size: 13px; color: var(--ink-2); line-height: 1.7; margin: 10px 0 0">
                {{ detail.summary.clinicalSummary || '暂无临床摘要。' }}
              </p>
            </div>
            <div class="card" style="padding: 14px">
              <div class="micro">患者信息</div>
              <div style="display: grid; gap: 6px; margin-top: 10px; font-size: 13px; color: var(--ink-2)">
                <span>患者标识：{{ detail.patient.idMasked || '--' }}</span>
                <span>性别 / 年龄：{{ detail.patient.gender || '--' }} / {{ detail.patient.age ?? '--' }}</span>
                <span>设备：{{ detail.image.sourceDevice || '--' }}</span>
              </div>
            </div>
            <div class="card" style="padding: 14px">
              <div class="micro">知识增强</div>
              <div style="margin-top: 10px; font-size: 13px; color: var(--ink-2)">{{ detail.summary.knowledgeVersion || '--' }}</div>
              <div style="margin-top: 8px; font-size: 12px; color: var(--ink-3)">
                {{ detail.rag.enabled ? '当前任务已启用知识增强。' : '当前任务未启用知识增强。' }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div style="display: flex; flex-direction: column; gap: 16px">
        <section class="card" style="overflow: hidden">
          <div class="card-head"><h3>病灶列表</h3></div>
          <div style="padding: 16px; display: flex; flex-direction: column; gap: 12px; max-height: 420px; overflow-y: auto">
            <div v-for="lesion in detail.summary.lesions" :key="lesion.id" class="card" style="padding: 14px; background: var(--surface-2)">
              <div style="display: flex; justify-content: space-between; gap: 12px; align-items: start">
                <div>
                  <div style="display: flex; gap: 8px; align-items: center">
                    <span class="chip" :style="lesionListChipStyle(lesion.severityCode)">{{ lesion.severityCode || 'UNKNOWN' }}</span>
                    <span class="mono" style="font-size: 11px; color: var(--ink-3)">{{ lesion.toothCode ? `牙位 ${lesion.toothCode}` : '未定位牙位' }}</span>
                  </div>
                  <div style="margin-top: 10px; font-size: 13px; color: var(--ink-2); line-height: 1.7">{{ lesion.summary || '暂无病灶描述。' }}</div>
                </div>
                <div style="text-align: right; font-size: 11px; color: var(--ink-3)">
                  <div>{{ lesion.areaRatio != null ? `${(lesion.areaRatio * 100).toFixed(2)}%` : '--' }}</div>
                  <div style="margin-top: 4px">{{ lesion.areaPx != null ? `${lesion.areaPx}px` : '--' }}</div>
                </div>
              </div>
              <div style="margin-top: 10px; font-size: 12px; color: var(--ink-3)">
                处理建议：{{ lesion.treatmentSuggestion || '请结合医生复核后确认。' }}
              </div>
            </div>
            <div v-if="detail.summary.lesions.length === 0" style="font-size: 13px; color: var(--ink-3)">当前结果没有结构化病灶列表。</div>
          </div>
        </section>

        <section class="card" style="overflow: hidden">
          <div class="card-head"><h3>治疗建议</h3></div>
          <div style="padding: 16px; display: flex; flex-direction: column; gap: 12px">
            <div class="card" style="padding: 14px; background: var(--brand-50); border-color: var(--brand-200)">
              <div class="micro" style="color: var(--brand-800)">知识增强建议</div>
              <div style="margin-top: 8px; font-size: 13px; color: var(--ink-2); line-height: 1.7">
                {{ detail.summary.followUpRecommendation || detail.rag.answer || '暂无建议。' }}
              </div>
            </div>
            <div
              v-for="(item, index) in detail.summary.treatmentPlan"
              :key="`${item.title}-${index}`"
              class="card"
              style="padding: 14px; background: var(--surface-2)"
            >
              <div style="display: flex; justify-content: space-between; gap: 10px; align-items: center">
                <strong style="font-size: 13px">{{ item.title }}</strong>
                <span class="chip chip-neutral">{{ item.priority }}</span>
              </div>
              <div style="margin-top: 8px; font-size: 12px; color: var(--ink-3); line-height: 1.7">{{ item.details }}</div>
            </div>
            <div v-if="detail.summary.treatmentPlan.length === 0" style="font-size: 13px; color: var(--ink-3)">当前结果没有结构化治疗计划。</div>
          </div>
        </section>

        <section class="card" style="overflow: hidden">
          <div class="card-head"><h3>知识引用</h3></div>
          <div style="padding: 16px; display: flex; flex-direction: column; gap: 12px; max-height: 280px; overflow-y: auto">
            <div v-for="citation in detail.summary.citations" :key="citation.index" class="card" style="padding: 14px; background: var(--surface-2)">
              <div style="display: flex; justify-content: space-between; gap: 8px; align-items: start">
                <strong style="font-size: 13px">{{ citation.title }}</strong>
                <span class="chip chip-neutral">#{{ citation.index }}</span>
              </div>
              <div style="margin-top: 8px; font-size: 12px; color: var(--ink-3); line-height: 1.7">{{ citation.excerpt || '无摘要片段。' }}</div>
              <div v-if="citation.sourceUri" class="mono" style="margin-top: 8px; font-size: 10px; color: var(--ink-4)">{{ citation.sourceUri }}</div>
            </div>
            <div v-if="detail.summary.citations.length === 0" style="font-size: 13px; color: var(--ink-3)">当前结果没有返回知识引用。</div>
          </div>
        </section>
      </div>
    </div>

    <section class="card" style="overflow: hidden">
      <div class="card-head"><h3>处理时间线</h3></div>
      <div style="padding: 16px; display: flex; flex-wrap: wrap; gap: 12px">
        <div v-for="node in detail.timeline" :key="node.code" class="card" style="padding: 14px; min-width: 180px; background: var(--surface-2)">
          <div style="display: flex; justify-content: space-between; gap: 10px; align-items: center">
            <strong style="font-size: 13px">{{ node.name }}</strong>
            <span class="chip" :style="timelineChipStyle(node.status)">{{ timelineStatusLabel(node.status) }}</span>
          </div>
          <div v-if="node.description" style="margin-top: 8px; font-size: 12px; color: var(--ink-3)">{{ node.description }}</div>
          <div v-if="node.time" class="mono" style="margin-top: 8px; font-size: 10px; color: var(--ink-4)">{{ formatDateTime(node.time) }}</div>
        </div>
        <div v-if="detail.timeline.length === 0" style="font-size: 13px; color: var(--ink-3)">当前没有可展示的时间线节点。</div>
      </div>
    </section>
  </div>

  <div v-else-if="store.loading" style="min-height: 60vh; display: grid; place-items: center">
    <div class="card" style="padding: 24px 28px; text-align: center">
      <div class="micro">Loading</div>
      <div style="margin-top: 8px; font-size: 14px; color: var(--ink-2)">正在加载分析详情...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import type { AnalysisLesion } from '@/models/analysis'
import { useAnalysisStore } from '@/stores/analysis'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()

const activeCanvas = ref('source')
const detail = computed(() => store.currentDetail)

const taskStatus = computed<'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED'>(() => {
  const status = (detail.value?.task.status || '').toUpperCase()
  if (status === 'SUCCESS' || status === 'DONE') return 'DONE'
  if (status === 'RUNNING') return 'RUNNING'
  if (status === 'REVIEW') return 'REVIEW'
  if (status === 'FAILED') return 'FAILED'
  return 'QUEUED'
})

const taskStatusLabel = (status: typeof taskStatus.value) => {
  switch (status) {
    case 'DONE':
      return '已完成'
    case 'RUNNING':
      return '运行中'
    case 'REVIEW':
      return '待复核'
    case 'FAILED':
      return '失败'
    default:
      return '排队中'
  }
}

const statusChipStyle = (status: typeof taskStatus.value) => {
  switch (status) {
    case 'DONE':
      return 'background: var(--ok-100); color: var(--ok-700);'
    case 'RUNNING':
      return 'background: var(--brand-100); color: var(--brand-800);'
    case 'REVIEW':
      return 'background: var(--warn-100); color: var(--warn-700);'
    case 'FAILED':
      return 'background: var(--danger-100); color: var(--danger-700);'
    default:
      return 'background: var(--bg-alt); color: var(--ink-2);'
  }
}

const canvasOptions = computed(() => {
  const options = [{ code: 'source', label: '原图', url: detail.value?.image.url }]
  for (const asset of detail.value?.task.visualAssets || []) {
    if (!asset.url) continue
    options.push({
      code: asset.type.toLowerCase(),
      label: asset.label,
      url: asset.url
    })
  }
  return options
})

const activeCanvasLabel = computed(() => canvasOptions.value.find((item) => item.code === activeCanvas.value)?.label || '原图')
const currentCanvasUrl = computed(() => canvasOptions.value.find((item) => item.code === activeCanvas.value)?.url)

const drawableLesions = computed(() =>
  (detail.value?.summary.lesions || []).filter((lesion) => Array.isArray(lesion.bbox) && lesion.bbox.length === 4)
)

const shouldDrawBoxes = computed(
  () =>
    activeCanvas.value === 'source' &&
    Boolean(detail.value?.summary.annotationImageWidth) &&
    Boolean(detail.value?.summary.annotationImageHeight) &&
    drawableLesions.value.length > 0
)

const lesionColor = (severityCode?: string) => {
  switch ((severityCode || '').toUpperCase()) {
    case 'C3':
      return '#e5483c'
    case 'C2':
      return '#e08a2c'
    default:
      return '#12a594'
  }
}

const lesionBorderStyle = (severityCode?: string) => {
  const color = lesionColor(severityCode)
  return `position:absolute; inset:0; border:2px solid ${color}; border-radius: 8px; box-shadow: 0 0 0 1px rgba(255,255,255,.06);`
}

const lesionBadgeStyle = (severityCode?: string) => {
  const color = lesionColor(severityCode)
  return `position:absolute; top:-24px; left:0; padding: 4px 8px; border-radius: 8px; background:${color}; color:#fff; font-family: var(--font-mono); font-size:10px; white-space: nowrap;`
}

const lesionListChipStyle = (severityCode?: string) => {
  const code = (severityCode || '').toUpperCase()
  if (code === 'C3') return 'background: var(--danger-100); color: var(--danger-700);'
  if (code === 'C2') return 'background: var(--warn-100); color: var(--warn-700);'
  return 'background: var(--brand-100); color: var(--brand-800);'
}

const lesionBoxStyle = (lesion: AnalysisLesion) => {
  const bbox = lesion.bbox || [0, 0, 0, 0]
  const imageWidth = detail.value?.summary.annotationImageWidth || 1
  const imageHeight = detail.value?.summary.annotationImageHeight || 1
  return {
    left: `${(bbox[0] / imageWidth) * 100}%`,
    top: `${(bbox[1] / imageHeight) * 100}%`,
    width: `${((bbox[2] - bbox[0]) / imageWidth) * 100}%`,
    height: `${((bbox[3] - bbox[1]) / imageHeight) * 100}%`
  }
}

const timelineStatusLabel = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return '完成'
    case 'CURRENT':
      return '当前'
    default:
      return '待处理'
  }
}

const timelineChipStyle = (status: string) => {
  if (status === 'COMPLETED') return 'background: var(--ok-100); color: var(--ok-700);'
  if (status === 'CURRENT') return 'background: var(--brand-100); color: var(--brand-800);'
  return 'background: var(--bg-alt); color: var(--ink-2);'
}

const formatDateTime = (value: string) => new Date(value).toLocaleString()

const openDoctorQa = () => {
  const taskNo = detail.value?.task.no || String(route.params.taskId || '')
  if (!taskNo) return
  router.push({ name: 'rag-console', query: { taskNo, mode: 'doctor' } })
}

const fetchDetail = async () => {
  const taskIdentifier = String(route.params.taskId || '')
  if (!taskIdentifier) return
  await store.fetchDetail(taskIdentifier)
}

watch(detail, () => {
  activeCanvas.value = 'source'
})

watch(
  () => route.params.taskId,
  async (taskId, previous) => {
    if (taskId && taskId !== previous) {
      await fetchDetail()
    }
  }
)

onMounted(fetchDetail)
</script>
