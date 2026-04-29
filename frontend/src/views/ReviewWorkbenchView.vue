<template>
  <div class="med-page review-page">
    <section class="med-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">Review Workbench</div>
        <h1 class="med-title">医生复核工作台</h1>
        <p class="med-subtitle">
          聚合待复核队列、影像标注与医生修订内容。你可以在这里完成分级调整、备注补充，并直接进入报告视图。
        </p>
      </div>
      <div class="med-action-row">
        <button class="med-btn med-btn--ghost" @click="goToDetail" :disabled="!currentWorkbench">
          <AppIcon name="scan" :size="14" />
          分析详情
        </button>
        <button class="med-btn med-btn--primary" @click="goToReport" :disabled="!currentWorkbench">
          <AppIcon name="report" :size="14" />
          报告视图
        </button>
      </div>
    </section>

    <section class="med-grid-3 review-layout">
      <aside class="med-card review-queue-card">
        <div class="med-card-inner">
          <div class="med-section-head">
            <h2 class="med-section-title">复核队列</h2>
            <span class="med-chip med-chip--warn">{{ filteredQueueItems.length }}</span>
          </div>
          <div class="med-search review-search">
            <AppIcon name="search" :size="14" />
            <input v-model.trim="queueKeyword" type="text" placeholder="搜索病例号或任务号" />
          </div>
          <div class="review-queue-list">
            <div v-if="queueLoading" class="med-empty">正在加载复核队列...</div>
            <button
              v-for="item in filteredQueueItems"
              :key="item.id"
              class="review-queue-item"
              :class="{ active: item.id === activeTaskId }"
              @click="selectTask(item.id)"
            >
              <div class="review-queue-head">
                <div>
                  <strong class="med-mono">{{ item.no }}</strong>
                  <div class="med-meta">{{ item.caseNo || item.patientName || 'CASE' }}</div>
                </div>
                <span class="med-chip" :class="gradeClass(getDisplayedQueueGrade(item))">{{ getDisplayedQueueGrade(item) }}</span>
              </div>
              <div class="review-queue-foot">
                <span class="med-meta med-mono">{{ formatTaskTime(item.createdAt) }}</span>
                <span class="med-meta">UC {{ (item.uncertainty || 0).toFixed(2) }}</span>
              </div>
            </button>
            <div v-if="!queueLoading && filteredQueueItems.length === 0" class="med-empty">当前没有待复核任务。</div>
          </div>
        </div>
      </aside>

      <section class="med-card review-image-card">
        <div class="med-card-inner">
          <div class="med-section-head">
            <h2 class="med-section-title">影像视图</h2>
            <div class="med-chip-row" v-if="currentWorkbench">
              <span class="med-chip">{{ currentWorkbench.caseInfo.caseNo }}</span>
              <span class="med-chip med-chip--accent">UC {{ currentWorkbench.aiResult.uncertaintyScore.toFixed(2) }}</span>
            </div>
          </div>

          <div v-if="currentWorkbench" class="review-image-stage">
            <div class="review-image-canvas">
              <template v-if="shouldRenderSyntheticImage">
                <div class="review-synthetic">
                  <div
                    v-for="tooth in syntheticTeeth"
                    :key="tooth.id"
                    :style="tooth.style"
                    class="review-synthetic-tooth"
                  ></div>
                  <div
                    v-for="hotspot in syntheticHotspots"
                    :key="hotspot.id"
                    :style="hotspot.style"
                    class="review-synthetic-hotspot"
                  ></div>
                </div>
              </template>
              <img
                v-else
                :src="currentWorkbench.image.imageUrl"
                alt="review xray"
                class="review-image"
              />

              <div
                v-for="box in currentWorkbench.aiResult.detections"
                :key="box.id"
                :style="boxStyle(box)"
                class="review-box review-box-ai"
              >
                <div class="review-box-label">AI {{ box.label }}</div>
              </div>

              <div
                v-for="box in renderedDoctorDetections"
                :key="box.id"
                :style="boxStyle(box)"
                class="review-box review-box-doctor"
              >
                <div class="review-box-label">DOC {{ box.label }}</div>
              </div>

              <div class="review-stage-badges">
                <span class="med-chip med-chip--warn">AI 标注 {{ currentWorkbench.aiResult.detections.length }}</span>
                <span class="med-chip med-chip--ok">医生修订 {{ renderedDoctorDetections.length }}</span>
              </div>
            </div>

            <div class="review-stage-summary">
              <article class="med-card review-mini-card">
                <div class="med-card-inner">
                  <div class="med-metric-label">AI 预测</div>
                  <div class="med-metric-value review-mini-value">{{ currentWorkbench.aiResult.gradingLabel }}</div>
                  <div class="med-metric-caption">模型给出的初始等级</div>
                </div>
              </article>
              <article class="med-card review-mini-card">
                <div class="med-card-inner">
                  <div class="med-metric-label">医生确认</div>
                  <div class="med-metric-value review-mini-value">{{ selectedGrade }}</div>
                  <div class="med-metric-caption">当前编辑中的修订等级</div>
                </div>
              </article>
            </div>
          </div>
          <div v-else class="med-empty">{{ workbenchLoading ? '正在加载复核详情...' : '请选择左侧任务开始复核。' }}</div>
        </div>
      </section>

      <section class="med-card review-editor-card">
        <div class="med-card-inner">
          <div class="med-section-head">
            <h2 class="med-section-title">复核编辑</h2>
            <span class="med-chip med-chip--accent" v-if="currentWorkbench">{{ currentWorkbench.task.taskNo }}</span>
          </div>

          <div v-if="currentWorkbench" class="review-editor-stack">
            <div class="review-split-card">
              <div>
                <span class="queue-label">原始草稿</span>
                <div class="review-grade-pill">{{ originalGrade }}</div>
                <p class="review-small-note">{{ originalReasonText }}</p>
              </div>
              <div>
                <span class="queue-label">当前编辑</span>
                <div class="review-grade-pill active">{{ selectedGrade }}</div>
                <p class="review-small-note">{{ selectedTags.length ? selectedTags.join(' / ') : '未选择修订原因' }}</p>
              </div>
            </div>

            <div class="med-input-wrap">
              <span class="queue-label">分级修订</span>
              <div class="med-tabs">
                <button
                  v-for="grade in currentWorkbench.reviewOptions.gradeOptions"
                  :key="grade"
                  class="med-tab"
                  :class="{ 'is-active': selectedGrade === grade }"
                  @click="selectedGrade = grade"
                >
                  {{ grade }}
                </button>
              </div>
            </div>

            <div class="med-input-wrap">
              <span class="queue-label">修订原因</span>
              <div class="med-tabs">
                <button
                  v-for="tag in currentWorkbench.reviewOptions.reasonTags"
                  :key="tag"
                  class="med-tab"
                  :class="{ 'is-active': selectedTags.includes(tag) }"
                  @click="toggleTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>

            <label class="med-input-wrap">
              <span class="queue-label">临床备注</span>
              <textarea v-model="clinicalNote" class="med-textarea review-textarea" placeholder="填写复核结论、分级调整依据或临床补充说明"></textarea>
            </label>

            <div class="med-note">
              原始备注：{{ originalNoteText }}
            </div>

            <div class="review-editor-actions">
              <button class="med-btn med-btn--ghost" @click="saveDraftLocally">
                <AppIcon name="pen" :size="14" />
                保存草稿
              </button>
              <button class="med-btn med-btn--ghost" @click="requestSecondOpinion">
                <AppIcon name="compare" :size="14" />
                二次意见
              </button>
              <button class="med-btn med-btn--primary" :disabled="submitting" @click="submitReview">
                <AppIcon name="check" :size="14" />
                {{ submitting ? '提交中...' : '提交复核' }}
              </button>
            </div>
          </div>
          <div v-else class="med-empty">{{ workbenchLoading ? '正在加载复核详情...' : '请选择左侧任务开始复核。' }}</div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { reviewApi } from '@/api/review'
import { analysisApi } from '@/api/analysis'
import type { AnalysisTaskItem } from '@/models/analysis'
import { useNotificationStore } from '@/stores/notification'
import { ApiClientError } from '@/api/request'
import { loadWorkspaceSettings } from '@/utils/workbenchSettings'

type ReviewGrade = 'G0' | 'G1' | 'G2' | 'G3' | 'G4'

interface DetectionBox {
  id: string
  x: number
  y: number
  width: number
  height: number
  label: ReviewGrade
  confidence?: number
}

interface ReviewWorkbenchData {
  task: {
    taskId: number
    taskNo?: string
    createdAt: string
    statusCode?: string
  }
  caseInfo: {
    caseId: number
    caseNo: string
    visitTime?: string
  }
  image: {
    imageId: number
    imageUrl: string
    sourceDevice?: string
  }
  aiResult: {
    gradingLabel: ReviewGrade
    uncertaintyScore: number
    detections: DetectionBox[]
  }
  doctorDraft?: {
    draftId?: number
    revisedGrade?: ReviewGrade
    revisedDetections?: DetectionBox[]
    reasonTags?: string[]
    note?: string
  }
  reviewOptions: {
    gradeOptions: ReviewGrade[]
    reasonTags: string[]
  }
}

interface DraftState {
  revisedGrade: ReviewGrade
  reasonTags: string[]
  note: string
}

interface SyntheticTooth {
  id: string
  style: string
}

interface SyntheticHotspot {
  id: string
  style: string
}

const route = useRoute()
const router = useRouter()
const notificationStore = useNotificationStore()
const settings = loadWorkspaceSettings()

const queueItems = ref<AnalysisTaskItem[]>([])
const queueKeyword = ref('')
const queueLoading = ref(false)
const workbenchLoading = ref(false)
const submitting = ref(false)
const activeTaskId = ref<number | null>(null)
const selectedGrade = ref<ReviewGrade>('G2')
const selectedTags = ref<string[]>([])
const clinicalNote = ref('')

const workbenchMap = ref<Record<number, ReviewWorkbenchData>>({})
const draftStateMap = ref<Record<number, DraftState>>({})

const routeTaskId = computed(() => Number.parseInt(route.params.taskId as string, 10) || 0)

const filteredQueueItems = computed(() => {
  const keyword = queueKeyword.value.trim().toLowerCase()
  if (!keyword) return queueItems.value
  return queueItems.value.filter((item) =>
    [item.no, item.caseNo, item.patientId, item.patientName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
})

const currentWorkbench = computed(() => {
  if (activeTaskId.value == null) return null
  return workbenchMap.value[activeTaskId.value] ?? null
})

const originalGrade = computed<ReviewGrade>(() =>
  normalizeGrade(currentWorkbench.value?.doctorDraft?.revisedGrade || currentWorkbench.value?.aiResult.gradingLabel)
)

const originalReasonText = computed(() => {
  const reasonTags = currentWorkbench.value?.doctorDraft?.reasonTags ?? []
  return reasonTags.length ? reasonTags.join(' / ') : '未记录'
})

const originalNoteText = computed(() => currentWorkbench.value?.doctorDraft?.note || '暂无原始备注')

const renderedDoctorDetections = computed(() => {
  if (!currentWorkbench.value) return []
  const baseDetections = currentWorkbench.value.doctorDraft?.revisedDetections?.length
    ? currentWorkbench.value.doctorDraft.revisedDetections
    : currentWorkbench.value.aiResult.detections.slice(0, 1).map((box) => ({
        ...box,
        id: `${box.id}-doctor`,
        y: Math.min(0.92, box.y + 0.05),
        height: Math.max(0.08, box.height - 0.05)
      }))

  return baseDetections.map((box, index) => ({
    ...box,
    id: box.id || `doc-box-${index + 1}`,
    label: selectedGrade.value
  }))
})

const shouldRenderSyntheticImage = computed(() => {
  const imageUrl = currentWorkbench.value?.image?.imageUrl ?? ''
  return !imageUrl || /mock|demo/i.test(imageUrl)
})

const syntheticTeeth = computed<SyntheticTooth[]>(() => {
  const seed = activeTaskId.value ?? 1
  return Array.from({ length: 4 }, (_, index) => {
    const width = 14 + ((seed + index) % 3) * 2
    const left = 12 + index * 18 + ((seed + index) % 2) * 2
    const height = 44 + ((seed + index) % 4) * 4
    return {
      id: `tooth-${index}`,
      style: `left:${left}%; width:${width}%; height:${height}%`
    }
  })
})

const syntheticHotspots = computed<SyntheticHotspot[]>(() => {
  const seed = activeTaskId.value ?? 1
  return [
    {
      id: 'hotspot-amber',
      style: `left:${44 + (seed % 4) * 2}%; top:${38 + (seed % 3) * 4}%; width:13%; height:16%; background:rgba(255,181,71,0.28)`
    },
    {
      id: 'hotspot-emerald',
      style: `left:${40 + (seed % 5)}%; top:${48 + (seed % 2) * 3}%; width:11%; height:12%; background:rgba(26,168,100,0.22)`
    }
  ]
})

const gradeClass = (grade?: string) => {
  switch ((grade || '').toUpperCase()) {
    case 'G0':
      return 'med-chip--ok'
    case 'G1':
      return 'med-chip--accent'
    case 'G2':
      return 'med-chip--warn'
    case 'G3':
    case 'G4':
      return 'med-chip--danger'
    default:
      return ''
  }
}

const formatTaskTime = (createdAt?: string) => {
  if (!createdAt) return '--:--'
  return new Date(createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const boxStyle = (box: DetectionBox) => ({
  left: `${box.x * 100}%`,
  top: `${box.y * 100}%`,
  width: `${box.width * 100}%`,
  height: `${box.height * 100}%`
})

const normalizeApiError = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message
  if (error instanceof Error) return error.message
  return '请求失败'
}

const normalizeGrade = (grade?: string): ReviewGrade => {
  switch (grade) {
    case 'G0':
    case 'G1':
    case 'G2':
    case 'G3':
    case 'G4':
      return grade
    default:
      return 'G0'
  }
}

const normalizeDetection = (box: Partial<DetectionBox>, index: number): DetectionBox => ({
  id: box.id || `box-${index + 1}`,
  x: typeof box.x === 'number' ? box.x : 0.3,
  y: typeof box.y === 'number' ? box.y : 0.3,
  width: typeof box.width === 'number' ? box.width : 0.18,
  height: typeof box.height === 'number' ? box.height : 0.16,
  label: normalizeGrade(box.label),
  confidence: typeof box.confidence === 'number' ? box.confidence : undefined
})

const normalizeWorkbench = (raw: ReviewWorkbenchData): ReviewWorkbenchData => ({
  ...raw,
  aiResult: {
    ...raw.aiResult,
    gradingLabel: normalizeGrade(raw.aiResult?.gradingLabel),
    uncertaintyScore: Number(raw.aiResult?.uncertaintyScore ?? 0),
    detections: (raw.aiResult?.detections ?? []).map((box, index) => normalizeDetection(box, index))
  },
  doctorDraft: raw.doctorDraft
    ? {
        ...raw.doctorDraft,
        revisedGrade: normalizeGrade(raw.doctorDraft.revisedGrade || raw.aiResult?.gradingLabel),
        revisedDetections: (raw.doctorDraft.revisedDetections ?? []).map((box, index) => normalizeDetection(box, index)),
        reasonTags: [...(raw.doctorDraft.reasonTags ?? [])],
        note: raw.doctorDraft.note || ''
      }
    : undefined,
  reviewOptions: {
    gradeOptions: (raw.reviewOptions?.gradeOptions ?? ['G0', 'G1', 'G2', 'G3', 'G4']).map((grade) => normalizeGrade(grade)),
    reasonTags: [...(raw.reviewOptions?.reasonTags ?? [])]
  }
})

const seedDraftFromWorkbench = (workbench: ReviewWorkbenchData): DraftState => ({
  revisedGrade: workbench.doctorDraft?.revisedGrade || workbench.aiResult.gradingLabel,
  reasonTags: [...(workbench.doctorDraft?.reasonTags || [])],
  note: workbench.doctorDraft?.note || ''
})

const persistCurrentDraft = () => {
  if (activeTaskId.value == null) return
  draftStateMap.value[activeTaskId.value] = {
    revisedGrade: selectedGrade.value,
    reasonTags: [...selectedTags.value],
    note: clinicalNote.value
  }
}

const hydrateDraft = (taskId: number) => {
  const workbench = workbenchMap.value[taskId]
  if (!workbench) return
  const draft = draftStateMap.value[taskId] ?? seedDraftFromWorkbench(workbench)
  draftStateMap.value[taskId] = {
    revisedGrade: draft.revisedGrade,
    reasonTags: [...draft.reasonTags],
    note: draft.note
  }
  selectedGrade.value = draft.revisedGrade
  selectedTags.value = [...draft.reasonTags]
  clinicalNote.value = draft.note
}

const upsertQueueItemFromWorkbench = (workbench: ReviewWorkbenchData) => {
  const item: AnalysisTaskItem = {
    id: workbench.task.taskId,
    no: workbench.task.taskNo || `T-${workbench.task.taskId}`,
    caseNo: workbench.caseInfo.caseNo,
    grade: workbench.aiResult.gradingLabel,
    uncertainty: workbench.aiResult.uncertaintyScore,
    status: 'REVIEW',
    createdAt: workbench.task.createdAt,
    needsReview: true
  }

  const next = [...queueItems.value]
  const index = next.findIndex((queueItem) => queueItem.id === item.id)
  if (index === -1) next.push(item)
  else next[index] = { ...next[index], ...item }
  next.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  queueItems.value = next
}

const loadWorkbench = async (taskId: number) => {
  workbenchLoading.value = true
  try {
    const res = await reviewApi.getReviewWorkbench(taskId)
    const workbench = normalizeWorkbench(res.data as ReviewWorkbenchData)
    workbenchMap.value[taskId] = workbench
    if (!draftStateMap.value[taskId]) {
      draftStateMap.value[taskId] = seedDraftFromWorkbench(workbench)
    }
    upsertQueueItemFromWorkbench(workbench)
  } catch (error) {
    notificationStore.error('加载复核详情失败', normalizeApiError(error))
  } finally {
    workbenchLoading.value = false
  }
}

const fetchQueue = async () => {
  queueLoading.value = true
  try {
    const res = await analysisApi.getTasks({ pageNo: 1, pageSize: 24 })
    const records = (res.data.records || res.data.list || []) as Array<{
      taskId: number
      taskNo?: string
      taskStatusCode?: string
      createdAt: string
      caseNo?: string
      patientName?: string
      patientId?: string
      gradingLabel?: string
      uncertaintyScore?: number
      needsReview?: boolean
    }>

    queueItems.value = records
      .filter((item) => ['REVIEW'].includes(String(item.taskStatusCode || '').toUpperCase()))
      .map((item): AnalysisTaskItem => ({
        id: Number(item.taskId),
        no: item.taskNo || `TASK-${item.taskId}`,
        caseNo: item.caseNo,
        patientName: item.patientName,
        patientId: item.patientId,
        grade: item.gradingLabel,
        uncertainty: item.uncertaintyScore,
        status: 'REVIEW',
        createdAt: item.createdAt,
        needsReview: item.needsReview ?? true
      }))
      .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  } catch (error) {
    notificationStore.error('加载复核队列失败', normalizeApiError(error))
  } finally {
    queueLoading.value = false
  }
}

const selectTask = async (taskId: number, updateRoute = true) => {
  if (activeTaskId.value === taskId && currentWorkbench.value) return

  persistCurrentDraft()
  activeTaskId.value = taskId

  if (updateRoute && String(route.params.taskId) !== String(taskId)) {
    router.replace(`/review/${taskId}`)
  }

  if (!workbenchMap.value[taskId]) {
    await loadWorkbench(taskId)
  }

  hydrateDraft(taskId)
}

const toggleTag = (tag: string) => {
  const next = new Set(selectedTags.value)
  if (next.has(tag)) next.delete(tag)
  else next.add(tag)
  selectedTags.value = [...next]
  persistCurrentDraft()
}

const saveDraftLocally = () => {
  persistCurrentDraft()
  notificationStore.info('草稿已保存', '当前复核内容已保存在本地状态。')
}

const requestSecondOpinion = () => {
  notificationStore.info('二次意见', '二次意见入口已保留，后续可继续接线。')
}

const getDraftGrade = (item: AnalysisTaskItem) => draftStateMap.value[item.id]?.revisedGrade
const getDisplayedQueueGrade = (item: AnalysisTaskItem): ReviewGrade => normalizeGrade(getDraftGrade(item) || item.grade)

const submitReview = async () => {
  if (!currentWorkbench.value || activeTaskId.value == null) return
  submitting.value = true
  persistCurrentDraft()

  try {
    await reviewApi.submitReview({
      taskId: currentWorkbench.value.task.taskId,
      revisedGrade: selectedGrade.value,
      reasonTags: selectedTags.value,
      note: clinicalNote.value
    })

    workbenchMap.value[activeTaskId.value] = {
      ...currentWorkbench.value,
      doctorDraft: {
        draftId: currentWorkbench.value.doctorDraft?.draftId,
        revisedGrade: selectedGrade.value,
        revisedDetections: renderedDoctorDetections.value,
        reasonTags: [...selectedTags.value],
        note: clinicalNote.value
      }
    }

    notificationStore.success('复核已提交', '当前任务的复核结果已提交。')
    hydrateDraft(activeTaskId.value)
  } catch (error) {
    notificationStore.error('提交失败', normalizeApiError(error))
  } finally {
    submitting.value = false
  }
}

const goToDetail = () => {
  if (!currentWorkbench.value) return
  router.push(`/analysis/${currentWorkbench.value.task.taskId}`)
}

const goToReport = () => {
  if (!currentWorkbench.value) return
  router.push(`/reports/${currentWorkbench.value.task.taskId}`)
}

onMounted(async () => {
  if (routeTaskId.value) {
    await loadWorkbench(routeTaskId.value)
    activeTaskId.value = routeTaskId.value
    hydrateDraft(routeTaskId.value)
  }

  await fetchQueue()

  if (!routeTaskId.value && settings.autoOpenNewestReview && queueItems.value[0]) {
    await selectTask(queueItems.value[0].id)
  }

  if (routeTaskId.value && !queueItems.value.find((item) => item.id === routeTaskId.value) && currentWorkbench.value) {
    upsertQueueItemFromWorkbench(currentWorkbench.value)
  }
})

watch(routeTaskId, async (taskId) => {
  if (taskId && taskId !== activeTaskId.value) {
    await selectTask(taskId, false)
  }
})

watch([selectedGrade, selectedTags, clinicalNote], () => {
  persistCurrentDraft()
}, { deep: true })
</script>

<style scoped>
.review-page {
  gap: 16px;
}

.review-layout {
  grid-template-columns: 300px minmax(0, 1.15fr) minmax(360px, 0.95fr);
  align-items: start;
}

.review-queue-list,
.review-editor-stack {
  display: grid;
  gap: 12px;
}

.review-search {
  margin-bottom: 14px;
}

.review-queue-item {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(112, 224, 255, 0.08);
  background: rgba(15, 31, 63, 0.42);
  color: var(--text);
  cursor: pointer;
  text-align: left;
  transition: all .18s ease;
}

.review-queue-item:hover,
.review-queue-item.active {
  border-color: rgba(0, 229, 255, 0.28);
  background: rgba(0, 229, 255, 0.12);
}

.review-queue-head,
.review-queue-foot,
.review-editor-actions,
.review-stage-summary,
.review-split-card,
.review-finding-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.review-queue-foot {
  margin-top: 10px;
  align-items: center;
}

.review-image-stage {
  display: grid;
  gap: 14px;
}

.review-image-canvas {
  position: relative;
  min-height: 620px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, #07141a, #020b10);
  border: 1px solid rgba(112, 224, 255, 0.12);
}

.review-image,
.review-synthetic {
  position: absolute;
  inset: 18px;
  border-radius: 16px;
}

.review-image {
  width: calc(100% - 36px);
  height: calc(100% - 36px);
  object-fit: contain;
  background: rgba(0,0,0,0.24);
}

.review-synthetic {
  overflow: hidden;
  background: radial-gradient(circle at center, rgba(255,255,255,0.08), rgba(255,255,255,0.02));
}

.review-synthetic-tooth,
.review-synthetic-hotspot {
  position: absolute;
}

.review-synthetic-tooth {
  bottom: 10%;
  background: linear-gradient(180deg, rgba(255,255,255,.35), rgba(255,255,255,.06));
  border-radius: 40% 40% 12% 12%;
  border: 1px solid rgba(255,255,255,.1);
}

.review-synthetic-hotspot {
  border-radius: 999px;
  filter: blur(18px);
}

.review-box {
  position: absolute;
  border-radius: 10px;
  pointer-events: none;
}

.review-box::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
}

.review-box-ai::before {
  border: 2px solid #f7a23a;
}

.review-box-doctor::before {
  border: 2px dashed #35f8ff;
}

.review-box-label {
  position: absolute;
  top: -24px;
  left: 0;
  padding: 4px 8px;
  border-radius: 8px;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 10px;
  font-weight: 700;
  color: #061936;
}

.review-box-ai .review-box-label {
  background: #f7a23a;
}

.review-box-doctor .review-box-label {
  background: #35f8ff;
}

.review-stage-badges {
  position: absolute;
  top: 18px;
  left: 18px;
  display: grid;
  gap: 8px;
}

.review-mini-card {
  flex: 1;
}

.review-mini-value {
  font-size: 22px;
}

.review-split-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(112, 224, 255, 0.08);
  background: rgba(15, 31, 63, 0.42);
}

.review-grade-pill {
  margin-top: 10px;
  display: inline-flex;
  min-width: 64px;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 12px;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.08);
  color: var(--text);
  font-size: 20px;
  font-weight: 800;
}

.review-grade-pill.active {
  background: rgba(0, 229, 255, 0.14);
  color: var(--accent);
}

.review-small-note {
  margin: 10px 0 0;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.review-textarea {
  min-height: 180px;
}

.review-editor-actions {
  justify-content: flex-end;
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-dim);
}

@media (max-width: 1360px) {
  .review-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .review-image-canvas {
    min-height: 460px;
  }
}
</style>

