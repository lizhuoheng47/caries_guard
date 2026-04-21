<template>
  <div class="page" style="max-width: none">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">{{ mode === 'doctor' ? 'Doctor QA' : 'Patient Explanation' }}</div>
        <h1 class="page-hello-title">智能解释与医生问答</h1>
      </div>
      <div class="lib-filter-group">
        <button class="lib-filter" :class="{ on: mode === 'patient' }" @click="changeMode('patient')">患者模式</button>
        <button class="lib-filter" :class="{ on: mode === 'doctor' }" @click="changeMode('doctor')">医生模式</button>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: 280px 1fr 320px; gap: 16px; min-height: 720px">
      <section class="card" style="display: flex; flex-direction: column; overflow: hidden">
        <div class="card-head">
          <h3>任务上下文</h3>
          <div class="card-head-actions">
            <span class="chip chip-neutral">{{ recentTasks.length }}</span>
          </div>
        </div>

        <div style="padding: 16px; display: flex; flex-direction: column; gap: 16px">
          <div>
            <div class="micro" style="margin-bottom: 8px">最近可问答任务</div>
            <div style="display: grid; gap: 8px; max-height: 190px; overflow-y: auto">
              <button
                v-for="task in recentTasks"
                :key="task.no"
                class="rag-task"
                :class="{ on: task.no === selectedTaskNo }"
                @click="selectTask(task.no)"
              >
                <div style="display: flex; justify-content: space-between; gap: 10px; align-items: start">
                  <div style="display: grid; gap: 4px; text-align: left">
                    <strong class="mono" style="font-size: 12px; color: var(--brand-700)">{{ task.no }}</strong>
                    <span style="font-size: 11px; color: var(--ink-3)">{{ formatDateTime(task.createdAt) }}</span>
                  </div>
                  <span class="chip" :style="taskStatusChipStyle(task.status)">{{ taskStatusLabel(task.status) }}</span>
                </div>
              </button>

              <div v-if="recentTasks.length === 0" class="card" style="padding: 14px; color: var(--ink-3)">
                当前没有可用于问答的分析任务。
              </div>
            </div>
          </div>

          <div v-if="contextLoading" class="card" style="padding: 16px; color: var(--ink-3)">
            正在加载任务上下文...
          </div>

          <template v-else-if="selectedDetail">
            <div
              style="aspect-ratio: 16 / 10; border-radius: 14px; overflow: hidden; background: linear-gradient(180deg, var(--surface-sunk), var(--surface)); border: 1px solid var(--line)"
            >
              <img
                v-if="contextPreviewUrl"
                :src="contextPreviewUrl"
                alt="task context preview"
                style="width: 100%; height: 100%; object-fit: cover"
              />
              <div v-else style="height: 100%; display: grid; place-items: center; color: var(--ink-3); font-size: 12px">
                当前任务没有可展示的影像资源
              </div>
            </div>

            <div style="display: grid; gap: 10px; font-size: 13px">
              <div style="display: flex; justify-content: space-between"><span class="micro">任务号</span><span class="mono">{{ selectedDetail.task.no }}</span></div>
              <div style="display: flex; justify-content: space-between"><span class="micro">患者</span><span>{{ selectedDetail.patient.name || selectedDetail.patient.idMasked || '--' }}</span></div>
              <div style="display: flex; justify-content: space-between"><span class="micro">基本信息</span><span>{{ patientInfoText }}</span></div>
              <div style="display: flex; justify-content: space-between"><span class="micro">病例号</span><span class="mono">{{ selectedDetail.caseInfo.no || '--' }}</span></div>
              <div style="display: flex; justify-content: space-between"><span class="micro">检查时间</span><span class="mono">{{ formatDateTime(selectedDetail.task.createdAt) }}</span></div>
            </div>

            <div class="card" style="padding: 14px; background: var(--warn-100); border-color: #f4d7ad">
              <div class="micro" style="margin-bottom: 6px; color: var(--warn-700)">AI 结论</div>
              <div style="display: flex; gap: 10px; align-items: start">
                <span class="chip" style="background: #fff; color: var(--warn-700)">{{ selectedDetail.summary.grade }}</span>
                <div style="display: grid; gap: 6px; font-size: 12px; color: var(--ink-2)">
                  <span>{{ contextSummaryText }}</span>
                  <span v-if="selectedDetail.summary.uncertainty != null" class="mono" style="font-size: 11px">
                    uncertainty={{ selectedDetail.summary.uncertainty.toFixed(2) }}
                  </span>
                </div>
              </div>
            </div>

            <div
              v-if="selectedDetail.summary.needsReview"
              class="card"
              style="padding: 12px 14px; background: var(--danger-100); border-color: #f2c7c4; color: var(--danger-700)"
            >
              当前任务已被标记为建议复核，问答内容仅作辅助说明。
            </div>

            <div style="display: flex; gap: 10px">
              <button class="btn btn-ghost" style="flex: 1; justify-content: center" @click="openAnalysisDetail">
                分析详情
              </button>
              <button class="btn btn-primary" style="flex: 1; justify-content: center" @click="useFirstPrompt" :disabled="promptSuggestions.length === 0">
                直接开始
              </button>
            </div>
          </template>

          <div v-else class="card" style="padding: 16px; color: var(--ink-3)">
            请选择左侧任务后再开始问答。
          </div>
        </div>
      </section>

      <section class="card" style="display: flex; flex-direction: column; overflow: hidden">
        <div class="card-head">
          <h3>{{ mode === 'doctor' ? '医生问答' : '患者解释' }}</h3>
          <div class="card-head-actions">
            <span class="micro">{{ selectedTaskNo || 'NO TASK' }}</span>
          </div>
        </div>

        <div
          style="padding: 16px 18px 0; border-bottom: 1px solid var(--line); background: #fff"
          v-if="selectedDetail"
        >
          <div class="micro" style="margin-bottom: 10px">建议问题</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px; padding-bottom: 16px">
            <button
              v-for="prompt in promptSuggestions"
              :key="prompt"
              class="lib-filter"
              @click="sendMessage(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </div>

        <div style="flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 14px; background: var(--surface-2)">
          <template v-for="(msg, index) in messages" :key="`${msg.role}-${index}`">
            <div
              :style="{
                display: 'flex',
                gap: '10px',
                maxWidth: '88%',
                alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row'
              }"
            >
              <div
                :style="{
                  width: '32px',
                  height: '32px',
                  borderRadius: '10px',
                  display: 'grid',
                  placeItems: 'center',
                  background: msg.role === 'user' ? 'var(--brand-100)' : 'var(--surface)',
                  border: '1px solid var(--line)',
                  flexShrink: 0
                }"
              >
                <AppIcon :name="msg.role === 'user' ? 'user' : 'sparkle'" :size="14" />
              </div>

              <div style="display: flex; flex-direction: column; gap: 8px">
                <div
                  :style="{
                    padding: '12px 14px',
                    borderRadius: '14px',
                    background: msg.role === 'user' ? 'var(--brand-700)' : 'var(--surface)',
                    color: msg.role === 'user' ? '#fff' : 'var(--ink-1)',
                    border: msg.role === 'user' ? '1px solid var(--brand-700)' : '1px solid var(--line)',
                    lineHeight: '1.7',
                    fontSize: '13px',
                    whiteSpace: 'pre-wrap'
                  }"
                >
                  {{ msg.content }}
                </div>

                <div
                  v-if="msg.warning"
                  class="card"
                  style="padding: 10px 12px; background: var(--warn-100); border-color: #f4d7ad; font-size: 12px; color: var(--warn-700)"
                >
                  {{ msg.warning }}
                </div>

                <div v-if="msg.meta" class="mono" style="font-size: 10px; color: var(--ink-4)">
                  {{ msg.meta }}
                </div>
              </div>
            </div>
          </template>

          <div v-if="loading" style="display: flex; gap: 10px; max-width: 85%">
            <div style="width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center; background: var(--surface); border: 1px solid var(--line)">
              <AppIcon name="sparkle" :size="14" />
            </div>
            <div class="card" style="padding: 12px 14px; background: #fff">正在生成回答...</div>
          </div>

          <div v-if="!selectedDetail && !contextLoading" class="card" style="padding: 16px; color: var(--ink-3)">
            当前没有选中的任务上下文，无法生成稳定回答。
          </div>
        </div>

        <div style="padding: 16px; border-top: 1px solid var(--line); background: #fff">
          <div class="micro" style="margin-bottom: 8px">{{ modeDescription }}</div>
          <div style="display: flex; gap: 10px">
            <textarea
              v-model="inputText"
              class="rag-input"
              :placeholder="inputPlaceholder"
              @keydown.enter.exact.prevent="sendMessage()"
            ></textarea>
            <button class="btn btn-primary" style="align-self: flex-end; height: 44px" :disabled="askDisabled" @click="sendMessage()">
              发送
              <AppIcon name="arrow_right" :size="14" />
            </button>
          </div>
          <div style="margin-top: 8px; font-size: 11px; color: var(--ink-3)">
            `Enter` 发送，`Shift + Enter` 换行。当前页面只调用 Java `/api/v1/rag/**`。
          </div>
        </div>
      </section>

      <section class="card" style="display: flex; flex-direction: column; overflow: hidden">
        <div class="card-head">
          <h3>引用来源</h3>
          <div class="card-head-actions">
            <span class="micro">{{ citations.length }} ITEMS</span>
          </div>
        </div>

        <div style="padding: 16px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; flex: 1">
          <div v-if="latestContextSummary" class="card" style="padding: 14px; background: var(--brand-50); border-color: var(--brand-200)">
            <div class="micro" style="margin-bottom: 8px; color: var(--brand-800)">上下文摘要</div>
            <div style="font-size: 12px; color: var(--ink-2); line-height: 1.7">{{ latestContextSummary }}</div>
          </div>

          <div v-for="(citation, index) in citations" :key="`${citation.id}-${index}`" class="card" style="padding: 14px">
            <div style="display: flex; justify-content: space-between; gap: 10px; align-items: start">
              <div style="display: flex; gap: 8px; align-items: center; min-width: 0">
                <span class="chip chip-neutral" style="padding: 0 8px">#{{ index + 1 }}</span>
                <span style="font-size: 13px; font-weight: 600; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                  {{ citation.docTitle || 'Knowledge citation' }}
                </span>
              </div>
              <span class="mono" style="font-size: 10px; color: var(--ink-3)">P{{ citation.pageNumber || '?' }}</span>
            </div>
            <p style="font-size: 12px; color: var(--ink-2); line-height: 1.7; margin: 10px 0 0">
              {{ citation.chunkText || '暂无摘要片段。' }}
            </p>
            <div v-if="citation.sourceUri" class="mono" style="margin-top: 8px; font-size: 10px; color: var(--ink-4)">
              {{ citation.sourceUri }}
            </div>
          </div>

          <div v-if="citations.length === 0" class="card" style="padding: 16px; color: var(--ink-3)">
            当前回答没有返回引用片段。优先追问“分级依据”“复核原因”“临床建议”，更容易命中知识引用。
          </div>

          <div v-if="latestSafetyMessages.length > 0" class="card" style="padding: 14px; background: var(--warn-100); border-color: #f4d7ad">
            <div class="micro" style="margin-bottom: 8px; color: var(--warn-700)">安全提示</div>
            <div style="display: grid; gap: 6px; font-size: 12px; color: var(--warn-700)">
              <div v-for="warning in latestSafetyMessages" :key="warning">{{ warning }}</div>
            </div>
          </div>

          <div v-if="latestRefusalReason" class="card" style="padding: 14px; background: var(--surface-2)">
            <div class="micro" style="margin-bottom: 8px">拒答原因</div>
            <div style="font-size: 12px; color: var(--ink-2); line-height: 1.7">{{ latestRefusalReason }}</div>
          </div>
        </div>

        <div style="padding: 16px; border-top: 1px solid var(--line); background: var(--surface-2); display: grid; gap: 8px">
          <div style="display: flex; justify-content: space-between"><span class="micro">知识库版本</span><span class="mono">{{ latestKbVersion || '--' }}</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">模型</span><span class="mono">{{ latestModelName || '--' }}</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">置信度</span><span class="mono">{{ latestConfidence !== null ? `${(latestConfidence * 100).toFixed(0)}%` : '--' }}</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">Fallback</span><span class="mono">{{ latestFallback === null ? '--' : latestFallback ? 'YES' : 'NO' }}</span></div>
          <div style="display: flex; justify-content: space-between; gap: 10px"><span class="micro">Trace ID</span><span class="mono" style="font-size: 10px; color: var(--ink-3)">{{ latestTraceId || '--' }}</span></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { AnalysisAdapter } from '@/api/adapters/analysis'
import { analysisApi } from '@/api/analysis'
import type { CitationDTO, RagAnswerDTO } from '@/api/dto/rag'
import { ragApi } from '@/api/rag'
import { ApiClientError } from '@/api/request'
import type { AnalysisDetail, AnalysisTaskItem } from '@/models/analysis'
import { useNotificationStore } from '@/stores/notification'

type RagMode = 'doctor' | 'patient'

interface Message {
  role: 'ai' | 'user'
  content: string
  warning?: string
  meta?: string
}

const route = useRoute()
const router = useRouter()
const notificationStore = useNotificationStore()

const mode = ref<RagMode>('doctor')
const inputText = ref('')
const loading = ref(false)
const contextLoading = ref(false)
const recentTasks = ref<AnalysisTaskItem[]>([])
const selectedTaskNo = ref('')
const selectedDetail = ref<AnalysisDetail | null>(null)
const messages = ref<Message[]>([])
const citations = ref<CitationDTO[]>([])
const latestKbVersion = ref('')
const latestConfidence = ref<number | null>(null)
const latestTraceId = ref('')
const latestModelName = ref('')
const latestFallback = ref<boolean | null>(null)
const latestContextSummary = ref('')
const latestSafetyMessages = ref<string[]>([])
const latestRefusalReason = ref('')

const normalizeMode = (value: unknown): RagMode => (String(value || '').toLowerCase() === 'patient' ? 'patient' : 'doctor')

const normalizeTaskStatus = (status?: string): AnalysisTaskItem['status'] => {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'DONE') return 'DONE'
  if (normalized === 'REVIEW') return 'REVIEW'
  if (normalized === 'RUNNING') return 'RUNNING'
  if (normalized === 'FAILED') return 'FAILED'
  return 'QUEUED'
}

const clearAnswerMeta = () => {
  citations.value = []
  latestKbVersion.value = ''
  latestConfidence.value = null
  latestTraceId.value = ''
  latestModelName.value = ''
  latestFallback.value = null
  latestContextSummary.value = ''
  latestSafetyMessages.value = []
  latestRefusalReason.value = ''
}

const taskStatusLabel = (status?: string) => {
  switch (normalizeTaskStatus(status)) {
    case 'DONE':
      return '已完成'
    case 'REVIEW':
      return '待复核'
    case 'RUNNING':
      return '运行中'
    case 'FAILED':
      return '失败'
    default:
      return '排队中'
  }
}

const taskStatusChipStyle = (status?: string) => {
  switch (normalizeTaskStatus(status)) {
    case 'DONE':
      return 'background: var(--ok-100); color: var(--ok-700);'
    case 'REVIEW':
      return 'background: var(--warn-100); color: var(--warn-700);'
    case 'RUNNING':
      return 'background: var(--brand-100); color: var(--brand-800);'
    case 'FAILED':
      return 'background: var(--danger-100); color: var(--danger-700);'
    default:
      return 'background: var(--bg-alt); color: var(--ink-2);'
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  return new Date(value).toLocaleString()
}

const normalizeApiError = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message
  if (error instanceof Error) return error.message
  return '请求失败'
}

const selectedTask = computed(() => recentTasks.value.find((task) => task.no === selectedTaskNo.value) || null)

const patientInfoText = computed(() => {
  if (!selectedDetail.value) return '--'
  const gender = selectedDetail.value.patient.gender || '--'
  const age = selectedDetail.value.patient.age != null ? `${selectedDetail.value.patient.age} 岁` : '--'
  return `${gender} / ${age}`
})

const contextPreviewUrl = computed(() => {
  if (!selectedDetail.value) return ''
  const annotatedAsset = selectedDetail.value.task.visualAssets.find(
    (asset) => asset.url && /(ANNOT|VISUAL|HEAT|MASK)/i.test(asset.type)
  )
  const firstAsset = selectedDetail.value.task.visualAssets.find((asset) => asset.url)
  return annotatedAsset?.url || selectedDetail.value.image.url || firstAsset?.url || ''
})

const contextSummaryText = computed(() => {
  if (!selectedDetail.value) return '当前任务缺少稳定摘要。'
  return (
    selectedDetail.value.summary.followUpRecommendation ||
    selectedDetail.value.summary.clinicalSummary ||
    `当前 AI 分级为 ${selectedDetail.value.summary.grade}，共识别 ${selectedDetail.value.summary.lesionCount} 个病灶。`
  )
})

const modeDescription = computed(() =>
  mode.value === 'doctor'
    ? '医生模式会优先输出分级依据、复核重点和临床建议。'
    : '患者模式会尽量转成患者能理解的解释，但不替代医生最终判断。'
)

const inputPlaceholder = computed(() =>
  mode.value === 'doctor'
    ? '追问分级依据、不确定性来源、重点牙位或临床建议'
    : '让系统用患者能理解的话解释结果、风险和下一步建议'
)

const askDisabled = computed(() => loading.value || contextLoading.value || !selectedDetail.value || !inputText.value.trim())

const promptSuggestions = computed(() => {
  if (!selectedDetail.value) return []
  const grade = selectedDetail.value.summary.grade || '未分级'
  const uncertainty =
    selectedDetail.value.summary.uncertainty != null ? selectedDetail.value.summary.uncertainty.toFixed(2) : '--'

  if (mode.value === 'doctor') {
    return [
      `为什么判定为 ${grade}？`,
      `不确定性 ${uncertainty} 的主要来源是什么？`,
      '哪些牙位或病灶最值得人工复核？',
    ]
  }

  return [
    '请用患者能听懂的话解释这次结果',
    '接下来建议做什么检查或治疗？',
    selectedDetail.value.summary.needsReview ? '为什么这次结果还需要进一步复核？' : '这个结果现在算严重吗？',
  ]
})

const syncRouteQuery = (taskNo = selectedTaskNo.value, nextMode = mode.value) => {
  const currentTaskNo = String(route.query.taskNo || '')
  const currentMode = normalizeMode(route.query.mode)
  if (currentTaskNo === taskNo && currentMode === nextMode) return

  router.replace({
    name: 'rag-console',
    query: {
      taskNo: taskNo || undefined,
      mode: nextMode,
    },
  })
}

const buildOpeningMessage = (): Message[] => {
  if (!selectedDetail.value) {
    return [
      {
        role: 'ai',
        content: '请选择一条已完成分析的任务后再开始问答。',
      },
    ]
  }

  const detail = selectedDetail.value
  const lesionText = `病灶 ${detail.summary.lesionCount} 个，异常牙位 ${detail.summary.abnormalToothCount} 个`
  const uncertaintyText =
    detail.summary.uncertainty != null ? `不确定性 ${detail.summary.uncertainty.toFixed(2)}` : '暂无不确定性分数'

  const content =
    mode.value === 'doctor'
      ? `已载入任务 ${detail.task.no}。\n当前 AI 分级为 ${detail.summary.grade}，${lesionText}，${uncertaintyText}。\n你可以继续追问分级依据、复核重点、不确定性来源和临床建议。\n\n${contextSummaryText.value}`
      : `已载入任务 ${detail.task.no}。\n我会尽量把当前影像分析结果转换成患者能理解的解释。\n当前结果提示 ${detail.summary.grade}，${lesionText}。\n\n${contextSummaryText.value}`

  return [
    {
      role: 'ai',
      content,
      warning: detail.summary.needsReview ? '当前任务已被标记为建议复核，回答内容仅作辅助说明。' : undefined,
      meta: `${taskStatusLabel(detail.task.status)} · ${formatDateTime(detail.task.createdAt)}`,
    },
  ]
}

const buildDoctorContext = (detail: AnalysisDetail): Record<string, unknown> => ({
  taskNo: detail.task.no,
  caseNo: detail.caseInfo.no,
  patientIdMasked: detail.patient.idMasked,
  patientNameMasked: detail.patient.name,
  gender: detail.patient.gender,
  age: detail.patient.age,
  grade: detail.summary.grade,
  confidenceScore: detail.summary.confidence,
  uncertaintyScore: detail.summary.uncertainty,
  needsReview: detail.summary.needsReview,
  lesionCount: detail.summary.lesionCount,
  abnormalToothCount: detail.summary.abnormalToothCount,
  riskLevel: detail.summary.riskLevel,
  clinicalSummary: detail.summary.clinicalSummary,
  followUpRecommendation: detail.summary.followUpRecommendation,
  knowledgeVersion: detail.summary.knowledgeVersion,
  lesions: detail.summary.lesions.slice(0, 6).map((lesion) => ({
    toothCode: lesion.toothCode,
    severityCode: lesion.severityCode,
    confidence: lesion.confidence,
    uncertainty: lesion.uncertainty,
    summary: lesion.summary,
    treatmentSuggestion: lesion.treatmentSuggestion,
  })),
})

const buildPatientCaseSummary = (detail: AnalysisDetail): Record<string, unknown> => ({
  taskNo: detail.task.no,
  caseNo: detail.caseInfo.no,
  patientIdMasked: detail.patient.idMasked,
  age: detail.patient.age,
  grade: detail.summary.grade,
  lesionCount: detail.summary.lesionCount,
  needsReview: detail.summary.needsReview,
  riskLevel: detail.summary.riskLevel,
  summary: detail.summary.clinicalSummary || detail.summary.followUpRecommendation,
  recommendation: detail.summary.followUpRecommendation,
})

const applyAnswerMeta = (data: RagAnswerDTO) => {
  citations.value = data.citations || []
  latestKbVersion.value = data.knowledgeVersion || ''
  latestConfidence.value = typeof data.confidence === 'number' ? data.confidence : null
  latestTraceId.value = data.traceId || ''
  latestModelName.value = data.modelName || ''
  latestFallback.value = typeof data.fallback === 'boolean' ? data.fallback : null
  latestContextSummary.value = data.caseContextSummary || ''
  latestSafetyMessages.value = data.safetyFlags || []
  latestRefusalReason.value = data.refusalReason || ''
}

const upsertRecentTask = (detail: AnalysisDetail) => {
  const task: AnalysisTaskItem = {
    id: detail.task.id,
    no: detail.task.no,
    caseNo: detail.caseInfo.no,
    grade: detail.summary.grade,
    uncertainty: detail.summary.uncertainty,
    status: normalizeTaskStatus(detail.task.status),
    createdAt: detail.task.createdAt,
    needsReview: detail.summary.needsReview,
  }

  const next = [...recentTasks.value]
  const index = next.findIndex((item) => item.no === task.no)
  if (index === -1) next.unshift(task)
  else next[index] = { ...next[index], ...task }
  recentTasks.value = next.slice(0, 10)
}

const resetConversation = () => {
  inputText.value = ''
  clearAnswerMeta()
  messages.value = buildOpeningMessage()
}

const loadTaskDetail = async (taskNo: string) => {
  contextLoading.value = true
  try {
    const res = await analysisApi.getTaskDetail(taskNo)
    selectedDetail.value = AnalysisAdapter.toDetail(res.data)
    upsertRecentTask(selectedDetail.value)
    resetConversation()
  } catch (error) {
    selectedDetail.value = null
    clearAnswerMeta()
    messages.value = [
      {
        role: 'ai',
        content: '任务上下文加载失败，请确认该任务已经有可读的分析结果。',
      },
    ]
    notificationStore.error('加载任务上下文失败', normalizeApiError(error))
  } finally {
    contextLoading.value = false
  }
}

const selectTask = async (taskNo: string, updateRoute = true) => {
  if (!taskNo) return
  if (selectedTaskNo.value === taskNo && selectedDetail.value?.task.no === taskNo) {
    if (updateRoute) syncRouteQuery(taskNo, mode.value)
    return
  }

  selectedTaskNo.value = taskNo
  if (updateRoute) syncRouteQuery(taskNo, mode.value)
  await loadTaskDetail(taskNo)
}

const fetchRecentTasks = async () => {
  try {
    const res = await analysisApi.getTasks({ pageNo: 1, pageSize: 12 })
    const allTasks = AnalysisAdapter.toTaskList(res.data).items.sort(
      (a, b) => +new Date(b.createdAt) - +new Date(a.createdAt)
    )
    const preferredTasks = allTasks.filter((task) => ['DONE', 'REVIEW'].includes(task.status))
    recentTasks.value = (preferredTasks.length ? preferredTasks : allTasks).slice(0, 10)

    const initialTaskNo = String(route.query.taskNo || '') || recentTasks.value[0]?.no || ''
    if (initialTaskNo) {
      await selectTask(initialTaskNo, false)
    } else {
      resetConversation()
    }
  } catch (error) {
    notificationStore.error('加载任务列表失败', normalizeApiError(error))
    resetConversation()
  }
}

const buildWarningText = (data: RagAnswerDTO) => {
  const warnings: string[] = []
  if (data.safetyFlag === '1') {
    warnings.push('当前回答仅供辅助参考，不能替代临床最终判断。')
  }
  if (data.fallback) {
    warnings.push('当前回答使用了回退策略，请结合分析详情一起判断。')
  }
  if (data.refusalReason) {
    warnings.push(`模型约束：${data.refusalReason}`)
  }
  return warnings.join(' ')
}

const sendMessage = async (preset?: string) => {
  const text = (preset || inputText.value).trim()
  if (!text || loading.value || !selectedDetail.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  try {
    const res =
      mode.value === 'doctor'
        ? await ragApi.doctorQa({
            question: text,
            topK: 3,
            taskNo: selectedDetail.value.task.no,
            relatedBizNo: selectedDetail.value.caseInfo.no,
            clinicalContext: buildDoctorContext(selectedDetail.value),
          })
        : await ragApi.patientExplanation({
            question: text,
            topK: 3,
            taskNo: selectedDetail.value.task.no,
            relatedBizNo: selectedDetail.value.caseInfo.no,
            caseSummary: buildPatientCaseSummary(selectedDetail.value),
            riskLevelCode: selectedDetail.value.summary.riskLevel,
          })

    const data = res.data
    applyAnswerMeta(data)

    messages.value.push({
      role: 'ai',
      content: data.answerText || data.answer || '当前没有可返回的答案。',
      warning: buildWarningText(data) || undefined,
      meta: [data.requestNo, data.latencyMs ? `${data.latencyMs} ms` : undefined].filter(Boolean).join(' · '),
    })
  } catch (error) {
    const message = normalizeApiError(error)
    notificationStore.error('问答请求失败', message)
    messages.value.push({
      role: 'ai',
      content: `获取问答结果失败：${message}`,
    })
  } finally {
    loading.value = false
  }
}

const useFirstPrompt = () => {
  if (promptSuggestions.value.length > 0) {
    void sendMessage(promptSuggestions.value[0])
  }
}

const changeMode = (nextMode: RagMode) => {
  if (mode.value === nextMode) return
  mode.value = nextMode
  syncRouteQuery(selectedTaskNo.value, nextMode)
  resetConversation()
}

const openAnalysisDetail = () => {
  if (!selectedTaskNo.value) return
  router.push(`/analysis/${selectedTaskNo.value}`)
}

onMounted(async () => {
  mode.value = normalizeMode(route.query.mode)
  await fetchRecentTasks()
})

watch(
  () => route.query.taskNo,
  async (taskNo) => {
    const nextTaskNo = String(taskNo || '')
    if (nextTaskNo && nextTaskNo !== selectedTaskNo.value) {
      await selectTask(nextTaskNo, false)
    }
  }
)

watch(
  () => route.query.mode,
  (value) => {
    const nextMode = normalizeMode(value)
    if (nextMode !== mode.value) {
      mode.value = nextMode
      resetConversation()
    }
  }
)
</script>

<style scoped>
.rag-task {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  text-align: left;
  transition: all .15s;
}

.rag-task:hover {
  border-color: var(--line-strong);
  background: var(--surface-2);
}

.rag-task.on {
  border-color: var(--brand-200);
  background: var(--brand-50);
}

.rag-input {
  width: 100%;
  min-height: 88px;
  padding: 12px 14px;
  resize: none;
  outline: none;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface-2);
  color: var(--ink-1);
  line-height: 1.65;
}

.rag-input:focus {
  border-color: var(--brand-500);
  box-shadow: var(--glow-brand);
}
</style>
