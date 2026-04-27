<template>
  <div class="med-page detail-page">
    <section class="med-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">
          <button class="med-btn med-btn--tiny med-btn--ghost" @click="goBack">
            <AppIcon name="chevron_right" :size="12" class="back-icon" />
            返回队列
          </button>
          <span>Analysis Detail</span>
        </div>
        <h1 class="med-title">{{ detail?.task.no || `TASK-${taskId}` }}</h1>
        <p class="med-subtitle">
          查看当前任务的影像输出、病灶证据、临床摘要和时间线，并可直接进入复核或生成结构化报告。
        </p>
      </div>
      <div class="med-action-row">
        <span class="med-chip" :class="gradeClass(detail?.summary.grade)">{{ detail?.summary.grade || '--' }}</span>
        <span class="med-chip" :class="statusClass(detail?.task.status)">{{ detail?.task.status || 'PENDING' }}</span>
        <button class="med-btn med-btn--ghost" @click="openReport" :disabled="!detail">
          <AppIcon name="report" :size="14" />
          生成报告
        </button>
        <button class="med-btn med-btn--primary" @click="openReview" :disabled="!detail">
          <AppIcon name="check" :size="14" />
          进入复核
        </button>
      </div>
    </section>

    <section v-if="loading" class="med-card">
      <div class="med-card-inner med-empty">正在加载分析详情...</div>
    </section>
    <section v-else-if="!detail" class="med-card">
      <div class="med-card-inner med-empty">未获取到分析详情。</div>
    </section>
    <template v-else>
      <section class="med-metric-grid">
        <article class="med-card med-metric-card">
          <div class="med-metric-label">Confidence</div>
          <div class="med-metric-value">{{ formatPercent(detail.summary.confidence) }}</div>
          <div class="med-metric-caption">低于 {{ Math.round(settings.confidenceThreshold * 100) }}% 时建议人工确认</div>
        </article>
        <article class="med-card med-metric-card">
          <div class="med-metric-label">Uncertainty</div>
          <div class="med-metric-value">{{ formatPercent(detail.summary.uncertainty) }}</div>
          <div class="med-metric-caption">高于 {{ Math.round(settings.riskAlertThreshold * 100) }}% 将标为高关注</div>
        </article>
        <article class="med-card med-metric-card">
          <div class="med-metric-label">Lesions</div>
          <div class="med-metric-value">{{ detail.summary.lesionCount }}</div>
          <div class="med-metric-caption">异常牙位 {{ detail.summary.abnormalToothCount }} 颗</div>
        </article>
        <article class="med-card med-metric-card">
          <div class="med-metric-label">Latency</div>
          <div class="med-metric-value">{{ formatMillis(detail.task.inferenceMillis) }}</div>
          <div class="med-metric-caption">任务创建于 {{ formatDateTime(detail.task.createdAt) }}</div>
        </article>
      </section>

      <section v-if="confidenceWarning" class="med-note detail-warning">
        当前结果置信度低于工作台阈值，建议结合复核工作台进行人工确认后再生成正式报告。
      </section>

      <section class="detail-main-grid">
        <article class="med-card">
          <div class="med-card-inner">
            <div class="med-section-head">
              <h2 class="med-section-title">影像输出</h2>
              <div class="med-tabs">
                <button
                  v-for="mode in visualModes"
                  :key="mode.key"
                  class="med-tab"
                  :class="{ 'is-active': selectedVisualKey === mode.key }"
                  @click="selectedVisualKey = mode.key"
                >
                  {{ mode.label }}
                </button>
              </div>
            </div>

            <div class="detail-image-stage">
              <img v-if="currentVisual?.url" :src="currentVisual.url" alt="analysis image" class="detail-image" />
              <div v-else class="med-empty detail-image-empty">暂无影像预览</div>
              <svg v-if="bboxOverlays.length" class="detail-overlay" :viewBox="`0 0 ${annotationWidth} ${annotationHeight}`" preserveAspectRatio="none">
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
                    stroke-dasharray="8 5"
                  />
                  <rect
                    :x="item.bbox[0]"
                    :y="Math.max(8, item.bbox[1] - 28)"
                    width="88"
                    height="22"
                    rx="10"
                    :fill="overlayColor(item.severityCode)"
                    opacity="0.95"
                  />
                  <text
                    :x="item.bbox[0] + 10"
                    :y="Math.max(22, item.bbox[1] - 13)"
                    fill="#03161c"
                    font-size="12"
                    font-weight="700"
                  >
                    {{ item.toothCode || item.id }}
                  </text>
                </g>
              </svg>
            </div>

            <div class="detail-asset-strip" v-if="extraAssets.length">
              <button
                v-for="asset in extraAssets"
                :key="asset.key"
                class="asset-pill"
                :class="{ active: selectedVisualKey === asset.key }"
                @click="selectedVisualKey = asset.key"
              >
                <span>{{ asset.label }}</span>
                <span class="med-mono">{{ asset.type }}</span>
              </button>
            </div>
          </div>
        </article>

        <aside class="detail-side-grid">
          <article class="med-card">
            <div class="med-card-inner">
              <div class="med-section-head compact-head">
                <h2 class="med-section-title">病例摘要</h2>
              </div>
              <div class="med-kv">
                <div class="med-kv-item"><span class="med-kv-key">患者</span><span class="med-kv-value">{{ detail.patient.name || detail.patient.idMasked || '--' }}</span></div>
                <div class="med-kv-item"><span class="med-kv-key">病例号</span><span class="med-kv-value med-mono">{{ detail.caseInfo.no || '--' }}</span></div>
                <div class="med-kv-item"><span class="med-kv-key">来源设备</span><span class="med-kv-value">{{ detail.image.sourceDevice || '--' }}</span></div>
                <div class="med-kv-item"><span class="med-kv-key">就诊时间</span><span class="med-kv-value med-mono">{{ formatDateTime(detail.caseInfo.visitTime) }}</span></div>
                <div class="med-kv-item"><span class="med-kv-key">完成时间</span><span class="med-kv-value med-mono">{{ formatDateTime(detail.task.completedAt) }}</span></div>
              </div>
            </div>
          </article>

          <article class="med-card">
            <div class="med-card-inner">
              <div class="med-section-head compact-head">
                <h2 class="med-section-title">临床摘要</h2>
              </div>
              <div class="med-chip-row">
                <span class="med-chip" :class="gradeClass(detail.summary.grade)">{{ detail.summary.grade }}</span>
                <span v-if="detail.summary.riskLevel" class="med-chip" :class="riskClass(detail.summary.riskLevel)">{{ detail.summary.riskLevel }}</span>
                <span v-if="detail.summary.needsReview" class="med-chip med-chip--warn">建议复核</span>
              </div>
              <p class="detail-summary-text">{{ detail.summary.clinicalSummary || '暂无临床摘要。' }}</p>
              <div class="detail-follow-up" v-if="detail.summary.followUpRecommendation">
                <span class="queue-label">FOLLOW-UP</span>
                <p>{{ detail.summary.followUpRecommendation }}</p>
              </div>
            </div>
          </article>
        </aside>
      </section>

      <section class="med-grid-2">
        <article class="med-card">
          <div class="med-card-inner">
            <div class="med-section-head">
              <h2 class="med-section-title">病灶清单</h2>
              <span class="med-chip">{{ detail.summary.lesions.length }}</span>
            </div>
            <div v-if="detail.summary.lesions.length" class="lesion-list">
              <article v-for="item in detail.summary.lesions" :key="item.id" class="lesion-card">
                <div class="lesion-head">
                  <div>
                    <div class="lesion-title">
                      <span>{{ item.toothCode || item.id }}</span>
                      <span class="med-chip" :class="gradeClass(item.severityCode)">{{ item.severityCode || 'UNKNOWN' }}</span>
                    </div>
                    <div class="med-meta">面积 {{ item.areaRatio ? `${(item.areaRatio * 100).toFixed(1)}%` : '--' }}</div>
                  </div>
                  <div class="lesion-badges">
                    <span v-if="item.confidence != null" class="med-chip">置信 {{ formatPercent(item.confidence) }}</span>
                    <span v-if="item.uncertainty != null" class="med-chip">不确定 {{ formatPercent(item.uncertainty) }}</span>
                  </div>
                </div>
                <p class="lesion-summary">{{ item.summary || '暂无结构化摘要。' }}</p>
                <div v-if="item.treatmentSuggestion" class="lesion-suggestion">{{ item.treatmentSuggestion }}</div>
              </article>
            </div>
            <div v-else class="med-empty">暂无病灶记录。</div>
          </div>
        </article>

        <article class="med-card">
          <div class="med-card-inner">
            <div class="med-section-head">
              <h2 class="med-section-title">证据与建议</h2>
              <span class="med-chip med-chip--accent">RAG</span>
            </div>
            <div class="evidence-stack">
              <div class="evidence-panel">
                <div class="queue-label">风险因素</div>
                <ul v-if="detail.summary.riskFactors.length" class="evidence-list">
                  <li v-for="item in detail.summary.riskFactors" :key="item">{{ item }}</li>
                </ul>
                <div v-else class="med-empty inline-empty">未返回结构化风险因素。</div>
              </div>
              <div class="evidence-panel">
                <div class="queue-label">治疗计划</div>
                <div v-if="detail.summary.treatmentPlan.length" class="treatment-list">
                  <article v-for="plan in detail.summary.treatmentPlan" :key="`${plan.priority}-${plan.title}`" class="treatment-card">
                    <div class="treatment-head">
                      <strong>{{ plan.title }}</strong>
                      <span class="med-chip">{{ plan.priority }}</span>
                    </div>
                    <p>{{ plan.details }}</p>
                  </article>
                </div>
                <div v-else class="med-empty inline-empty">暂无治疗计划输出。</div>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section class="med-grid-2">
        <article class="med-card">
          <div class="med-card-inner">
            <div class="med-section-head">
              <h2 class="med-section-title">证据引用</h2>
              <span class="med-chip">{{ detail.summary.citations.length }}</span>
            </div>
            <div v-if="detail.summary.citations.length" class="citation-list">
              <article v-for="citation in detail.summary.citations" :key="`${citation.index}-${citation.title}`" class="citation-card">
                <div class="citation-head">
                  <strong>{{ citation.title }}</strong>
                  <span v-if="citation.score != null" class="med-chip">{{ formatPercent(citation.score) }}</span>
                </div>
                <p>{{ citation.excerpt || '无摘要内容。' }}</p>
                <div class="med-meta med-mono">{{ citation.sourceUri || 'mock://citation' }}</div>
              </article>
            </div>
            <div v-else class="med-empty">暂无证据引用。</div>
          </div>
        </article>

        <article class="med-card">
          <div class="med-card-inner">
            <div class="med-section-head">
              <h2 class="med-section-title">处理时间线</h2>
            </div>
            <div v-if="detail.timeline.length" class="timeline-list">
              <article v-for="node in detail.timeline" :key="node.code" class="timeline-card">
                <div class="timeline-marker" :class="node.status.toLowerCase()"></div>
                <div class="timeline-body">
                  <div class="timeline-head">
                    <strong>{{ node.name }}</strong>
                    <span class="med-chip">{{ node.status }}</span>
                  </div>
                  <div class="med-meta med-mono">{{ formatDateTime(node.time) }}</div>
                  <p>{{ node.description || '暂无补充描述。' }}</p>
                </div>
              </article>
            </div>
            <div v-else class="med-empty">暂无时间线。</div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import type { AnalysisLesion } from '@/models/analysis'
import { useAnalysisStore } from '@/stores/analysis'
import { loadWorkspaceSettings, type WorkspaceSettings } from '@/utils/workbenchSettings'

const route = useRoute()
const router = useRouter()
const analysisStore = useAnalysisStore()
const { currentDetail, loading } = storeToRefs(analysisStore)

const settings = loadWorkspaceSettings() as WorkspaceSettings
const selectedVisualKey = ref('')
const taskId = computed(() => String(route.params.taskId || ''))
const detail = computed(() => currentDetail.value)
const annotationWidth = computed(() => detail.value?.summary.annotationImageWidth || 512)
const annotationHeight = computed(() => detail.value?.summary.annotationImageHeight || 256)

const visualModes = computed(() => {
  const base = [] as Array<{ key: string; label: string; url?: string; type: string }>
  if (detail.value?.image.url) {
    base.push({ key: 'original', label: '原始影像', url: detail.value.image.url, type: 'IMAGE' })
  }
  ;(detail.value?.task.visualAssets || []).forEach((asset) => {
    base.push({
      key: asset.type.toLowerCase(),
      label: asset.label,
      url: asset.url,
      type: asset.type,
    })
  })
  return base.filter((item, index, list) => list.findIndex((entry) => entry.key === item.key) === index)
})

const currentVisual = computed(() => visualModes.value.find((item) => item.key === selectedVisualKey.value) || visualModes.value[0])
const extraAssets = computed(() => visualModes.value.filter((item) => item.key !== 'original'))
const bboxOverlays = computed<(AnalysisLesion & { bbox: [number, number, number, number] })[]>(() =>
  (detail.value?.summary.lesions || []).filter(
    (item): item is AnalysisLesion & { bbox: [number, number, number, number] } =>
      Array.isArray(item.bbox) && item.bbox.length === 4
  )
)
const confidenceWarning = computed(() => Number(detail.value?.summary.confidence || 0) > 0 && Number(detail.value?.summary.confidence || 0) < settings.confidenceThreshold)

const goBack = () => router.push('/analysis')
const openReview = () => detail.value && router.push(`/review/${detail.value.task.id}`)
const openReport = () => detail.value && router.push(`/reports/${detail.value.task.id}`)

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

const statusClass = (status?: string) => {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'DONE') return 'med-chip--ok'
  if (normalized === 'RUNNING') return 'med-chip--accent'
  if (normalized === 'REVIEW') return 'med-chip--warn'
  if (normalized === 'FAILED') return 'med-chip--danger'
  return ''
}

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
      return '#5eead4'
    default:
      return '#2ee6c8'
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

const loadDetail = async () => {
  if (!taskId.value) return
  try {
    await analysisStore.fetchDetail(taskId.value)
  } catch (error) {
    console.error('Failed to load analysis detail', error)
  }
}

watch(
  () => visualModes.value,
  (modes) => {
    if (!modes.length) {
      selectedVisualKey.value = ''
      return
    }
    const preferred = modes.find((item) => item.key === settings.defaultImageMode) || modes[0]
    if (!modes.find((item) => item.key === selectedVisualKey.value)) {
      selectedVisualKey.value = preferred.key
    }
  },
  { immediate: true }
)

onMounted(() => {
  void loadDetail()
})

watch(taskId, () => {
  void loadDetail()
})
</script>

<style scoped>
.detail-page {
  gap: 16px;
}

.back-icon {
  transform: rotate(180deg);
}

.detail-warning {
  border-color: rgba(247, 162, 58, 0.24);
  background: rgba(247, 162, 58, 0.08);
}

.detail-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.detail-side-grid,
.evidence-stack,
.citation-list,
.timeline-list,
.lesion-list,
.treatment-list {
  display: grid;
  gap: 12px;
}

.detail-image-stage {
  position: relative;
  min-height: 420px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: linear-gradient(180deg, rgba(2, 19, 26, 0.92), rgba(1, 10, 14, 0.98));
}

.detail-image {
  width: 100%;
  height: 100%;
  min-height: 420px;
  object-fit: contain;
  display: block;
}

.detail-overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.detail-image-empty {
  height: 420px;
  display: grid;
  place-items: center;
}

.detail-asset-strip {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.asset-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: rgba(11, 36, 44, 0.42);
  color: var(--text-soft);
  cursor: pointer;
}

.asset-pill.active {
  border-color: rgba(46, 230, 200, 0.32);
  background: rgba(46, 230, 200, 0.12);
  color: var(--accent);
}

.compact-head {
  margin-bottom: 10px;
}

.detail-summary-text,
.detail-follow-up p,
.lesion-summary,
.lesion-suggestion,
.treatment-card p,
.citation-card p,
.timeline-body p {
  margin: 0;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-soft);
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-dim);
  text-transform: uppercase;
}

.detail-follow-up {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: rgba(11, 36, 44, 0.46);
  border: 1px solid rgba(94, 234, 212, 0.08);
}

.lesion-card,
.treatment-card,
.citation-card,
.timeline-card,
.evidence-panel {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(94, 234, 212, 0.08);
  background: rgba(11, 36, 44, 0.42);
}

.lesion-head,
.lesion-title,
.lesion-badges,
.citation-head,
.treatment-head,
.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.lesion-title {
  align-items: center;
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.lesion-badges {
  justify-content: flex-end;
}

.lesion-summary {
  margin-top: 12px;
}

.lesion-suggestion {
  margin-top: 10px;
  color: var(--accent-2);
}

.evidence-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.inline-empty {
  margin-top: 10px;
  padding: 18px 14px;
}

.timeline-card {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
}

.timeline-marker {
  margin-top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(94, 234, 212, 0.2);
  box-shadow: 0 0 12px rgba(94, 234, 212, 0.18);
}

.timeline-marker.completed { background: #2ee6c8; }
.timeline-marker.current { background: #f7a23a; }
.timeline-marker.pending { background: #5e8a82; }

.timeline-body {
  display: grid;
  gap: 6px;
}

@media (max-width: 1180px) {
  .detail-main-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
