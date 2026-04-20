<template>
  <div class="page" style="max-width: none">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">Review Workbench</div>
        <h1 class="page-hello-title">医生复核工作台</h1>
      </div>
      <div v-if="currentWorkbench" style="display: flex; gap: 10px; align-items: center">
        <span class="chip chip-neutral">{{ currentWorkbench.caseInfo.caseNo }}</span>
        <span class="chip" :style="gradeChipStyle(currentWorkbench.aiResult.gradingLabel)">
          {{ currentWorkbench.aiResult.gradingLabel }}
        </span>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: 280px minmax(0, 1.2fr) minmax(360px, .95fr); gap: 16px; min-height: 760px">
      <aside class="card" style="overflow: hidden">
        <div class="card-head">
          <h3>复核队列</h3>
          <div class="card-head-actions">
            <span class="chip chip-neutral">{{ filteredQueueItems.length }}</span>
          </div>
        </div>
        <div style="padding: 16px; border-bottom: 1px solid var(--line)">
          <div class="lib-search" style="width: 100%">
            <AppIcon name="search" :size="14" />
            <input v-model.trim="queueKeyword" type="text" placeholder="搜索病例号或任务号" />
          </div>
        </div>
        <div style="padding: 12px; display: flex; flex-direction: column; gap: 10px; overflow-y: auto; max-height: 650px">
          <div v-if="queueLoading" class="card" style="padding: 14px; color: var(--ink-3)">正在加载复核队列...</div>
          <button
            v-for="item in filteredQueueItems"
            :key="item.id"
            class="card"
            :style="queueItemStyle(item.id === activeTaskId)"
            @click="selectTask(item.id)"
          >
            <div style="display: flex; justify-content: space-between; gap: 8px; align-items: start">
              <div style="display: grid; gap: 4px; text-align: left">
                <strong class="mono" style="font-size: 12px; color: var(--brand-700)">{{ item.no }}</strong>
                <span style="font-size: 12px; color: var(--ink-2)">{{ item.caseNo || 'CASE' }}</span>
              </div>
              <span class="mono" style="font-size: 10px; color: var(--ink-3)">{{ formatTaskTime(item.createdAt) }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px">
              <span class="chip" :style="gradeChipStyle(getDisplayedQueueGrade(item))">{{ getDisplayedQueueGrade(item) }}</span>
              <span class="mono" style="font-size: 11px; color: var(--ink-3)">UC {{ (item.uncertainty || 0).toFixed(2) }}</span>
            </div>
          </button>
          <div v-if="!queueLoading && filteredQueueItems.length === 0" class="card" style="padding: 14px; color: var(--ink-3)">当前没有待复核任务。</div>
        </div>
      </aside>

      <section class="card" style="overflow: hidden">
        <div class="card-head">
          <h3>影像视图</h3>
          <div class="card-head-actions" v-if="currentWorkbench">
            <span class="micro">UNCERTAINTY {{ currentWorkbench.aiResult.uncertaintyScore.toFixed(2) }}</span>
          </div>
        </div>
        <div v-if="currentWorkbench" style="padding: 18px">
          <div style="position: relative; min-height: 620px; border-radius: 18px; overflow: hidden; background: linear-gradient(180deg, #0d1215, #1d2a31); border: 1px solid #23343d">
            <div style="position: absolute; inset: 18px; border-radius: 14px; overflow: hidden; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.24)">
              <template v-if="shouldRenderSyntheticImage">
                <div style="position: relative; width: 72%; height: 78%">
                  <div
                    v-for="tooth in syntheticTeeth"
                    :key="tooth.id"
                    :style="`${tooth.style}; position:absolute; bottom:10%; background:linear-gradient(180deg, rgba(255,255,255,.35), rgba(255,255,255,.06)); border-radius:40% 40% 12% 12%; border:1px solid rgba(255,255,255,.1)`"
                  ></div>
                  <div
                    v-for="hotspot in syntheticHotspots"
                    :key="hotspot.id"
                    :style="`${hotspot.style}; position:absolute; border-radius:999px; filter:blur(18px)`"
                  ></div>
                </div>
              </template>
              <img
                v-else
                :src="currentWorkbench.image.imageUrl"
                alt="review xray"
                style="max-width: 100%; max-height: 100%; object-fit: contain"
              />
            </div>

            <div
              v-for="box in currentWorkbench.aiResult.detections"
              :key="box.id"
              :style="boxStyle(box)"
              style="position: absolute; pointer-events: none"
            >
              <div style="position: absolute; inset: 0; border: 2px solid #e08a2c; border-radius: 8px"></div>
              <div style="position: absolute; top: -24px; left: 0; padding: 4px 8px; border-radius: 8px; background: #e08a2c; color: #fff; font-family: var(--font-mono); font-size: 10px">
                AI {{ box.label }}
              </div>
            </div>

            <div
              v-for="box in renderedDoctorDetections"
              :key="box.id"
              :style="boxStyle(box)"
              style="position: absolute; pointer-events: none"
            >
              <div style="position: absolute; inset: 0; border: 2px dashed #1aa864; border-radius: 8px"></div>
              <div style="position: absolute; top: -24px; left: 0; padding: 4px 8px; border-radius: 8px; background: #1aa864; color: #fff; font-family: var(--font-mono); font-size: 10px">
                DOC {{ box.label }}
              </div>
            </div>

            <div style="position: absolute; top: 18px; left: 18px; display: grid; gap: 8px">
              <span class="chip" style="background: rgba(255,255,255,.88); color: var(--warn-700)">AI 标注 {{ currentWorkbench.aiResult.detections.length }}</span>
              <span class="chip" style="background: rgba(255,255,255,.88); color: var(--ok-700)">医生修订 {{ renderedDoctorDetections.length }}</span>
            </div>
          </div>
        </div>
        <div v-else style="padding: 24px; color: var(--ink-3)">
          {{ workbenchLoading ? '正在加载复核详情...' : '请选择左侧任务开始复核。' }}
        </div>
      </section>

      <section class="card" style="overflow: hidden">
        <div class="card-head">
          <h3>复核编辑</h3>
          <div class="card-head-actions" v-if="currentWorkbench">
            <span class="micro">{{ currentWorkbench.task.taskNo }}</span>
          </div>
        </div>

        <div v-if="currentWorkbench" style="padding: 18px; display: flex; flex-direction: column; gap: 16px">
          <div class="card" style="padding: 14px; background: var(--surface-2)">
            <div style="display: grid; grid-template-columns: 1fr auto 1fr; gap: 10px; align-items: center">
              <div style="text-align: center">
                <div class="micro">AI 预测</div>
                <div style="margin-top: 8px">
                  <span class="chip" :style="gradeChipStyle(currentWorkbench.aiResult.gradingLabel)">{{ currentWorkbench.aiResult.gradingLabel }}</span>
                </div>
              </div>
              <AppIcon name="arrow_right" :size="16" />
              <div style="text-align: center">
                <div class="micro">医生确认</div>
                <div style="margin-top: 8px">
                  <span class="chip" :style="gradeChipStyle(selectedGrade)">{{ selectedGrade }}</span>
                </div>
              </div>
            </div>
          </div>

          <div>
            <div class="micro" style="margin-bottom: 8px">分级修订</div>
            <div class="lib-filter-group" style="flex-wrap: wrap">
              <button
                v-for="grade in currentWorkbench.reviewOptions.gradeOptions"
                :key="grade"
                class="lib-filter"
                :class="{ on: selectedGrade === grade }"
                @click="selectedGrade = grade"
              >
                {{ grade }}
              </button>
            </div>
          </div>

          <div>
            <div class="micro" style="margin-bottom: 8px">修订原因</div>
            <div class="lib-filter-group" style="flex-wrap: wrap">
              <button
                v-for="tag in currentWorkbench.reviewOptions.reasonTags"
                :key="tag"
                class="lib-filter"
                :class="{ on: selectedTags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
            <div class="card" style="padding: 14px; background: var(--surface-2)">
              <div class="micro">原始草稿</div>
              <div style="display: grid; gap: 6px; margin-top: 10px; font-size: 12px; color: var(--ink-2)">
                <div>分级：{{ originalGrade }}</div>
                <div>原因：{{ originalReasonText }}</div>
                <div style="white-space: pre-line; color: var(--ink-3)">{{ originalNoteText }}</div>
              </div>
            </div>
            <div class="card" style="padding: 14px; background: var(--surface-2)">
              <div class="micro">当前编辑</div>
              <div style="display: grid; gap: 6px; margin-top: 10px; font-size: 12px; color: var(--ink-2)">
                <div>分级：{{ selectedGrade }}</div>
                <div>原因：{{ selectedTags.length ? selectedTags.join(' / ') : '未选择' }}</div>
                <div style="white-space: pre-line; color: var(--ink-3)">{{ clinicalNote || '暂无备注' }}</div>
              </div>
            </div>
          </div>

          <label style="display: grid; gap: 8px">
            <span class="micro">临床备注</span>
            <textarea
              v-model="clinicalNote"
              class="review-input"
              placeholder="填写复核结论、分级调整原因或其他临床说明"
            ></textarea>
          </label>

          <div style="display: flex; gap: 10px; justify-content: end; margin-top: auto">
            <button class="btn btn-ghost" @click="saveDraftLocally">保存草稿</button>
            <button class="btn btn-ghost" @click="requestSecondOpinion">二次意见</button>
            <button class="btn btn-primary" :disabled="submitting" @click="submitReview">
              {{ submitting ? '提交中...' : '提交复核' }}
            </button>
          </div>
        </div>

        <div v-else style="padding: 24px; color: var(--ink-3)">
          {{ workbenchLoading ? '正在加载复核详情...' : '请选择左侧任务开始复核。' }}
        </div>
      </section>
    </div>
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

const queueItemStyle = (active: boolean) =>
  `padding: 14px; text-align:left; background:${active ? 'var(--brand-50)' : '#fff'}; border-color:${active ? 'var(--brand-200)' : 'var(--line)'};`

const gradeChipStyle = (grade?: string) => {
  switch ((grade || '').toUpperCase()) {
    case 'G0':
      return 'background: var(--ok-100); color: var(--ok-700);'
    case 'G1':
      return 'background: var(--brand-100); color: var(--brand-800);'
    case 'G2':
      return 'background: var(--warn-100); color: var(--warn-700);'
    case 'G3':
    case 'G4':
      return 'background: var(--danger-100); color: var(--danger-700);'
    default:
      return 'background: var(--bg-alt); color: var(--ink-2);'
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
    }>

    queueItems.value = records
      .filter((item) => ['REVIEW'].includes(String(item.taskStatusCode || '').toUpperCase()))
      .map((item): AnalysisTaskItem => ({
        id: Number(item.taskId),
        no: item.taskNo || `TASK-${item.taskId}`,
        status: 'REVIEW',
        createdAt: item.createdAt,
        needsReview: true
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

onMounted(async () => {
  if (routeTaskId.value) {
    await loadWorkbench(routeTaskId.value)
    activeTaskId.value = routeTaskId.value
    hydrateDraft(routeTaskId.value)
  }
  await fetchQueue()
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
.review-input {
  width: 100%;
  min-height: 140px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface-2);
  color: var(--ink-1);
  resize: vertical;
  outline: none;
}

.review-input:focus {
  border-color: var(--brand-500);
  box-shadow: var(--glow-brand);
}
</style>
