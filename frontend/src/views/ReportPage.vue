<template>
  <div class="med-page report-page">
    <section class="med-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">
          <span>Diagnostic Report</span>
          <span v-if="detail" class="med-chip med-chip--ok">RP-{{ detail.task.id }}</span>
        </div>
        <h1 class="med-title">结构化诊断报告</h1>
        <p class="med-subtitle">
          将分析详情整理为临床阅读友好的报告文档，支持打印、复制摘要，并按系统设置决定是否包含引用与治疗计划。
        </p>
      </div>
      <div class="med-action-row">
        <button class="med-btn med-btn--ghost" @click="goToDetail" :disabled="!detail">
          <AppIcon name="scan" :size="14" />
          分析详情
        </button>
        <button class="med-btn med-btn--ghost" @click="goToReview" :disabled="!detail">
          <AppIcon name="check" :size="14" />
          复核工作台
        </button>
        <button class="med-btn med-btn--ghost" @click="copySummary" :disabled="!detail">
          <AppIcon name="share" :size="14" />
          复制摘要
        </button>
        <button class="med-btn med-btn--primary" @click="printReport" :disabled="!detail">
          <AppIcon name="download" :size="14" />
          打印 / 导出
        </button>
      </div>
    </section>

    <section class="med-card">
      <div class="med-card-inner report-toolbar">
        <div class="med-stack-inline">
          <span class="med-chip" :class="gradeClass(detail?.summary.grade)">{{ detail?.summary.grade || '--' }}</span>
          <span v-if="detail?.summary.riskLevel" class="med-chip" :class="riskClass(detail?.summary.riskLevel)">{{ detail.summary.riskLevel }}</span>
          <span class="med-chip med-chip--accent">TASK {{ activeTaskId || '--' }}</span>
        </div>
        <div class="med-stack-inline">
          <button class="med-tab" :class="{ 'is-active': includeTreatmentPlan }" @click="includeTreatmentPlan = !includeTreatmentPlan">治疗计划</button>
          <button class="med-tab" :class="{ 'is-active': includeCitations }" @click="includeCitations = !includeCitations">证据引用</button>
        </div>
      </div>
    </section>

    <section v-if="loading" class="med-card">
      <div class="med-card-inner med-empty">正在生成报告视图...</div>
    </section>
    <section v-else-if="!detail" class="med-card">
      <div class="med-card-inner med-empty">没有可展示的分析任务，请先从分析详情或任务队列进入。</div>
    </section>

    <section v-else class="med-card report-doc-wrap">
      <div class="report-doc">
        <header class="report-head">
          <div>
            <div class="med-eyebrow">DentAI Clinical Report</div>
            <h2>龋齿影像辅助诊断报告</h2>
            <div class="report-head-meta">
              <span class="med-mono">RP-{{ detail.task.id }}</span>
              <span>·</span>
              <span class="med-mono">{{ formatDateTime(detail.task.completedAt || detail.task.createdAt) }}</span>
            </div>
          </div>
          <div class="report-sign">
            <div class="report-sign-badge">
              <AppIcon name="logo" :size="18" />
            </div>
            <div>
              <div class="report-sign-title">DentAI Workstation</div>
              <div class="med-meta">AI analysis + doctor review workflow</div>
            </div>
          </div>
        </header>

        <section class="report-grid report-grid--info">
          <article class="report-info-card">
            <span class="queue-label">患者</span>
            <strong>{{ detail.patient.name || detail.patient.idMasked || '--' }}</strong>
            <span class="med-meta med-mono">{{ detail.patient.idMasked || '--' }}</span>
          </article>
          <article class="report-info-card">
            <span class="queue-label">病例号</span>
            <strong>{{ detail.caseInfo.no || '--' }}</strong>
            <span class="med-meta">{{ detail.image.sourceDevice || 'PANORAMIC' }}</span>
          </article>
          <article class="report-info-card">
            <span class="queue-label">总体分级</span>
            <strong>{{ detail.summary.grade }}</strong>
            <span class="med-meta">风险 {{ detail.summary.riskLevel || '--' }}</span>
          </article>
          <article class="report-info-card">
            <span class="queue-label">任务编号</span>
            <strong class="med-mono">{{ detail.task.no }}</strong>
            <span class="med-meta">耗时 {{ formatMillis(detail.task.inferenceMillis) }}</span>
          </article>
        </section>

        <section class="report-section">
          <div class="report-section-head">
            <span>01</span>
            <h3>影像概览</h3>
          </div>
          <div class="report-image-stage">
            <img v-if="detail.image.url" :src="detail.image.url" alt="report image" class="report-image" />
            <div v-else class="med-empty report-image-empty">暂无影像</div>
            <svg v-if="bboxOverlays.length" class="report-overlay" :viewBox="`0 0 ${annotationWidth} ${annotationHeight}`" preserveAspectRatio="none">
              <g v-for="(item, index) in bboxOverlays" :key="item.id || index">
                <rect
                  :x="item.bbox[0]"
                  :y="item.bbox[1]"
                  :width="item.bbox[2] - item.bbox[0]"
                  :height="item.bbox[3] - item.bbox[1]"
                  :stroke="overlayColor(item.severityCode)"
                  stroke-width="3"
                  fill="none"
                  rx="8"
                />
                <text :x="item.bbox[0] + 4" :y="item.bbox[1] - 8" :fill="overlayColor(item.severityCode)" font-size="12" font-weight="700">
                  {{ item.toothCode || item.id }}
                </text>
              </g>
            </svg>
          </div>
        </section>

        <section class="report-section">
          <div class="report-section-head">
            <span>02</span>
            <h3>摘要结论</h3>
          </div>
          <div class="report-grid report-grid--summary">
            <article class="report-summary-card">
              <span class="queue-label">置信度</span>
              <strong>{{ formatPercent(detail.summary.confidence) }}</strong>
            </article>
            <article class="report-summary-card">
              <span class="queue-label">不确定性</span>
              <strong>{{ formatPercent(detail.summary.uncertainty) }}</strong>
            </article>
            <article class="report-summary-card">
              <span class="queue-label">病灶数</span>
              <strong>{{ detail.summary.lesionCount }}</strong>
            </article>
            <article class="report-summary-card">
              <span class="queue-label">异常牙位</span>
              <strong>{{ detail.summary.abnormalToothCount }}</strong>
            </article>
          </div>
          <div class="report-paragraph">{{ detail.summary.clinicalSummary || '暂无结构化临床摘要。' }}</div>
          <div class="report-note" v-if="detail.summary.followUpRecommendation">
            <span class="queue-label">建议</span>
            <p>{{ detail.summary.followUpRecommendation }}</p>
          </div>
        </section>

        <section class="report-section">
          <div class="report-section-head">
            <span>03</span>
            <h3>病灶清单</h3>
          </div>
          <div v-if="detail.summary.lesions.length" class="report-findings">
            <article v-for="item in detail.summary.lesions" :key="item.id" class="report-finding-card">
              <div class="report-finding-head">
                <div>
                  <strong>{{ item.toothCode || item.id }}</strong>
                  <div class="med-meta">{{ item.severityCode || 'UNKNOWN' }}</div>
                </div>
                <div class="med-chip-row">
                  <span class="med-chip" :class="gradeClass(item.severityCode)">{{ item.severityCode || '--' }}</span>
                  <span v-if="item.confidence != null" class="med-chip">{{ formatPercent(item.confidence) }}</span>
                </div>
              </div>
              <p>{{ item.summary || '暂无病灶描述。' }}</p>
              <div class="report-note" v-if="item.treatmentSuggestion">
                <span class="queue-label">处理建议</span>
                <p>{{ item.treatmentSuggestion }}</p>
              </div>
            </article>
          </div>
          <div v-else class="med-empty">暂无病灶结构化数据。</div>
        </section>

        <section v-if="includeTreatmentPlan" class="report-section">
          <div class="report-section-head">
            <span>04</span>
            <h3>治疗计划</h3>
          </div>
          <div v-if="detail.summary.treatmentPlan.length" class="report-findings">
            <article v-for="plan in detail.summary.treatmentPlan" :key="`${plan.priority}-${plan.title}`" class="report-finding-card">
              <div class="report-finding-head">
                <strong>{{ plan.title }}</strong>
                <span class="med-chip">{{ plan.priority }}</span>
              </div>
              <p>{{ plan.details }}</p>
            </article>
          </div>
          <div v-else class="med-empty">暂无治疗计划。</div>
        </section>

        <section v-if="includeCitations" class="report-section">
          <div class="report-section-head">
            <span>05</span>
            <h3>证据引用</h3>
          </div>
          <div v-if="detail.summary.citations.length" class="report-findings">
            <article v-for="citation in detail.summary.citations" :key="`${citation.index}-${citation.title}`" class="report-finding-card">
              <div class="report-finding-head">
                <strong>{{ citation.title }}</strong>
                <span class="med-chip" v-if="citation.score != null">{{ formatPercent(citation.score) }}</span>
              </div>
              <p>{{ citation.excerpt || '暂无引用摘要。' }}</p>
              <div class="med-meta med-mono">{{ citation.sourceUri || 'mock://citation' }}</div>
            </article>
          </div>
          <div v-else class="med-empty">暂无证据引用。</div>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { analysisApi } from '@/api/analysis'
import type { AnalysisDetail, AnalysisLesion } from '@/models/analysis'
import { useNotificationStore } from '@/stores/notification'
import { loadWorkspaceSettings } from '@/utils/workbenchSettings'

const route = useRoute()
const router = useRouter()
const notificationStore = useNotificationStore()
const settings = loadWorkspaceSettings()

const loading = ref(false)
const detail = ref<AnalysisDetail | null>(null)
const activeTaskId = ref<string>('')
const includeCitations = ref(settings.reportIncludeCitations)
const includeTreatmentPlan = ref(settings.reportIncludeTreatmentPlan)

const annotationWidth = computed(() => detail.value?.summary.annotationImageWidth || 512)
const annotationHeight = computed(() => detail.value?.summary.annotationImageHeight || 256)
const bboxOverlays = computed<(AnalysisLesion & { bbox: [number, number, number, number] })[]>(() =>
  (detail.value?.summary.lesions || []).filter(
    (item): item is AnalysisLesion & { bbox: [number, number, number, number] } =>
      Array.isArray(item.bbox) && item.bbox.length === 4
  )
)

const overlayColor = (severity?: string) => {
  switch (String(severity || '').toUpperCase()) {
    case 'C3':
    case 'C4':
    case 'G3':
    case 'G4':
      return '#ff636e'
    case 'C2':
    case 'G2':
      return '#f7a23a'
    case 'C1':
    case 'G1':
      return '#3f79ff'
    default:
      return '#35f8ff'
  }
}

const gradeClass = (grade?: string) => {
  switch (String(grade || '').toUpperCase()) {
    case 'C0':
    case 'G0':
      return 'med-chip--ok'
    case 'C1':
    case 'G1':
      return 'med-chip--accent'
    case 'C2':
    case 'G2':
      return 'med-chip--warn'
    case 'C3':
    case 'C4':
    case 'G3':
    case 'G4':
      return 'med-chip--danger'
    default:
      return ''
  }
}

const riskClass = (risk?: string) => {
  const normalized = String(risk || '').toUpperCase()
  if (normalized.includes('高') || normalized === 'HIGH') return 'med-chip--danger'
  if (normalized.includes('中') || normalized === 'MEDIUM') return 'med-chip--warn'
  return 'med-chip--ok'
}

const resolveTaskId = async () => {
  const fromParam = String(route.params.taskId || '')
  if (fromParam) return fromParam
  const fromQuery = typeof route.query.taskId === 'string' ? route.query.taskId : ''
  if (fromQuery) return fromQuery

  const tasksRes = await analysisApi.getTasks({ pageNo: 1, pageSize: 1 })
  const first = tasksRes.data.records?.[0] || tasksRes.data.list?.[0]
  return first ? String(first.taskId) : ''
}

const loadReport = async () => {
  loading.value = true
  try {
    const taskId = await resolveTaskId()
    activeTaskId.value = taskId
    if (!taskId) {
      detail.value = null
      return
    }
    const res = await analysisApi.getTaskDetail(taskId)
    const task = res.data
    detail.value = {
      task: {
        id: Number(task.task.taskId),
        no: task.task.taskNo,
        status: task.task.taskStatusCode,
        createdAt: task.task.createdAt,
        completedAt: task.task.completedAt,
        inferenceMillis: task.task.inferenceMillis,
        visualAssets: (task.task.visualAssets || []).map((asset) => ({
          type: asset.assetTypeCode,
          label: asset.assetTypeLabel || asset.assetTypeCode,
          url: asset.accessUrl,
          toothCode: asset.toothCode,
          attachmentId: asset.attachmentId,
        })),
      },
      patient: {
        idMasked: task.patient?.patientIdMasked,
        name: task.patient?.patientNameMasked,
        gender: task.patient?.gender,
        age: task.patient?.age,
      },
      caseInfo: {
        id: task.caseInfo?.caseId,
        no: task.caseInfo?.caseNo,
        visitTime: task.caseInfo?.visitTime,
      },
      image: {
        id: task.image?.imageId,
        url: task.image?.imageUrl,
        sourceDevice: task.image?.sourceDevice,
      },
      summary: {
        grade: task.analysisSummary?.gradingLabel || task.analysisSummary?.overallHighestSeverity || 'UNKNOWN',
        confidence: task.analysisSummary?.confidenceScore ?? task.rawResultJson?.confidenceScore,
        uncertainty: task.analysisSummary?.uncertaintyScore ?? task.rawResultJson?.uncertaintyScore,
        needsReview: Boolean(task.analysisSummary?.needsReview ?? task.rawResultJson?.needsReview),
        riskLevel: task.analysisSummary?.riskLevelLabel || task.analysisSummary?.riskLevel || task.rawResultJson?.riskLevel,
        riskFactors: Array.isArray(task.rawResultJson?.riskFactors)
          ? task.rawResultJson.riskFactors.map((item: any) => item.factorName || item).filter(Boolean)
          : [],
        lesionCount: Number(task.analysisSummary?.lesionCount ?? task.rawResultJson?.lesionCount ?? 0),
        abnormalToothCount: Number(task.analysisSummary?.abnormalToothCount ?? task.rawResultJson?.abnormalToothCount ?? 0),
        clinicalSummary: task.rawResultJson?.clinicalSummary,
        followUpRecommendation: task.analysisSummary?.followUpRecommendation || task.rawResultJson?.followUpRecommendation,
        treatmentPlan: Array.isArray(task.rawResultJson?.treatmentPlan) ? task.rawResultJson.treatmentPlan : [],
        lesions: Array.isArray(task.rawResultJson?.lesionResults)
          ? task.rawResultJson.lesionResults.map((item: any, index: number) => ({
              id: item.id || `lesion-${index + 1}`,
              toothCode: item.toothCode,
              severityCode: item.severityCode,
              confidence: item.confidenceScore,
              uncertainty: item.uncertaintyScore,
              areaPx: item.lesionAreaPx,
              areaRatio: item.lesionAreaRatio,
              bbox: item.bbox,
              polygon: item.polygon,
              summary: item.summary,
              treatmentSuggestion: item.treatmentSuggestion,
            }))
          : [],
        citations: Array.isArray(task.analysisSummary?.citations || task.rawResultJson?.citations)
          ? (task.analysisSummary?.citations || task.rawResultJson?.citations).map((item: any, index: number) => ({
              index: item.rankNo ?? index + 1,
              title: item.docTitle || item.title || `Citation ${index + 1}`,
              excerpt: item.chunkText || item.content,
              score: item.score,
              sourceUri: item.sourceUri,
            }))
          : [],
        annotationImageWidth: task.rawResultJson?.annotationImageWidth,
        annotationImageHeight: task.rawResultJson?.annotationImageHeight,
        rawResultJson: task.rawResultJson,
      },
      timeline: [],
    }
  } catch (error) {
    console.error('Failed to load report', error)
    detail.value = null
  } finally {
    loading.value = false
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString()
}

const formatPercent = (value?: number) => {
  if (typeof value !== 'number') return '--'
  return `${Math.round(value * 100)}%`
}

const formatMillis = (value?: number) => {
  if (typeof value !== 'number' || value <= 0) return '--'
  return `${(value / 1000).toFixed(2)}s`
}

const copySummary = async () => {
  if (!detail.value) return
  const text = [
    `任务: ${detail.value.task.no}`,
    `病例: ${detail.value.caseInfo.no || '--'}`,
    `分级: ${detail.value.summary.grade}`,
    `摘要: ${detail.value.summary.clinicalSummary || '暂无摘要'}`,
    `建议: ${detail.value.summary.followUpRecommendation || '暂无建议'}`,
  ].join('\n')

  try {
    await navigator.clipboard.writeText(text)
    notificationStore.success('摘要已复制', '报告摘要已复制到剪贴板。')
  } catch {
    notificationStore.warning('复制失败', '当前浏览器环境不支持剪贴板写入。')
  }
}

const printReport = () => {
  window.print()
}

const goToDetail = () => detail.value && router.push(`/analysis/${detail.value.task.id}`)
const goToReview = () => detail.value && router.push(`/review/${detail.value.task.id}`)

onMounted(() => {
  void loadReport()
})

watch(() => route.fullPath, () => {
  void loadReport()
})
</script>

<style scoped>
.report-page {
  gap: 16px;
}

.report-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.report-doc-wrap {
  overflow: visible;
}

.report-doc {
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(18, 41, 75, 0.92), rgba(10, 18, 40, 0.96));
  color: #f2f7ff;
}

.report-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(112, 224, 255, 0.12);
}

.report-head h2 {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.05;
}

.report-head-meta {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  color: rgba(197, 216, 247, 0.62);
  font-size: 12px;
}

.report-sign {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-sign-badge {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(0, 229, 255, 0.16);
  color: #21cdfd;
}

.report-sign-title {
  font-weight: 700;
}

.report-grid {
  display: grid;
  gap: 14px;
}

.report-grid--info {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 24px;
}

.report-grid--summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.report-info-card,
.report-summary-card,
.report-finding-card,
.report-note {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(15, 31, 63, 0.72);
}

.report-info-card strong,
.report-summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.report-section {
  margin-top: 28px;
}

.report-section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.report-section-head span {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(0, 229, 255, 0.14);
  color: #35f8ff;
  font-weight: 800;
}

.report-section-head h3 {
  margin: 0;
  font-size: 20px;
}

.report-image-stage {
  position: relative;
  min-height: 360px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, #061936, #031020);
  border: 1px solid rgba(112, 224, 255, 0.08);
}

.report-image {
  width: 100%;
  height: 360px;
  object-fit: contain;
  display: block;
}

.report-overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.report-image-empty {
  height: 360px;
  display: grid;
  place-items: center;
}

.report-paragraph,
.report-findings p,
.report-note p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(216, 229, 251, 0.82);
}

.report-paragraph {
  margin-top: 16px;
}

.report-note {
  margin-top: 14px;
}

.report-findings {
  display: grid;
  gap: 12px;
}

.report-finding-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: rgba(197, 216, 247, 0.52);
}

@media (max-width: 1080px) {
  .report-grid--info,
  .report-grid--summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .report-doc {
    padding: 18px;
  }

  .report-head {
    flex-direction: column;
  }

  .report-grid--info,
  .report-grid--summary {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media print {
  .report-page > :not(.report-doc-wrap) {
    display: none !important;
  }

  .report-doc-wrap {
    border: 0;
    box-shadow: none;
  }

  .report-doc {
    background: #fff;
    padding: 0;
  }
}
</style>

