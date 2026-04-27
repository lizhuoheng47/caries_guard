<template>
  <div class="med-page queue-page">
    <section class="med-hero queue-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">
          <span>Analysis Queue</span>
          <span class="med-chip med-chip--accent">AUTO {{ autoRefreshLabel }}</span>
        </div>
        <h1 class="med-title">分析任务队列</h1>
        <p class="med-subtitle">
          汇总 AI 推理、待复核与已完成任务，支持快速筛选、自动刷新与直达详情/复核/报告流程。
        </p>
      </div>
      <div class="med-action-row">
        <button class="med-btn med-btn--ghost" @click="router.push('/cases')">
          <AppIcon name="library" :size="14" />
          病例中心
        </button>
        <button class="med-btn med-btn--primary" :disabled="loading" @click="reload">
          <AppIcon name="scan" :size="14" />
          {{ loading ? '刷新中' : '刷新队列' }}
        </button>
      </div>
    </section>

    <section class="med-metric-grid">
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Total Tasks</div>
        <div class="med-metric-value">{{ total }}</div>
        <div class="med-metric-caption">当前分析仓中的全部任务</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Review Queue</div>
        <div class="med-metric-value">{{ reviewCount }}</div>
        <div class="med-metric-caption">需要医生关注的任务</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Running</div>
        <div class="med-metric-value">{{ runningCount }}</div>
        <div class="med-metric-caption">模型正在推理中的任务</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Avg Latency</div>
        <div class="med-metric-value">{{ averageLatency }}</div>
        <div class="med-metric-caption">当前页任务平均推理耗时</div>
      </article>
    </section>

    <section class="med-card">
      <div class="med-card-inner queue-filter-shell">
        <div class="med-search queue-search">
          <AppIcon name="search" :size="15" />
          <input v-model.trim="keyword" type="text" placeholder="搜索任务号、病例号、患者、模型版本" />
        </div>
        <div class="med-tabs">
          <button
            v-for="item in tabs"
            :key="item.code"
            class="med-tab"
            :class="{ 'is-active': statusFilter === item.code }"
            @click="statusFilter = item.code"
          >
            <span>{{ item.label }}</span>
            <span class="med-mono">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="med-grid-2 queue-body">
      <article class="med-card">
        <div class="med-card-inner">
          <div class="med-section-head">
            <h2 class="med-section-title">任务列表</h2>
            <div class="med-stack-inline">
              <span class="med-meta">第 {{ pageNo }} / {{ totalPages }} 页</span>
              <span class="med-chip">显示 {{ displayedTasks.length }} 项</span>
            </div>
          </div>

          <div v-if="loading" class="med-empty">正在同步分析任务...</div>
          <div v-else-if="displayedTasks.length === 0" class="med-empty">当前筛选条件下没有匹配任务。</div>
          <div v-else class="queue-list">
            <article
              v-for="task in displayedTasks"
              :key="task.taskId"
              class="queue-item"
              :class="[`status-${normalizeStatus(task.taskStatusCode)}`]"
              @click="openDetail(task.taskId)"
            >
              <div class="queue-item-head">
                <div>
                  <div class="queue-item-title">
                    <span class="med-mono">{{ task.taskNo || `TASK-${task.taskId}` }}</span>
                    <span class="med-chip" :class="statusClass(task.taskStatusCode)">{{ statusLabel(task.taskStatusCode) }}</span>
                  </div>
                  <p class="queue-item-sub">
                    {{ task.patientName || task.caseNo || '未命名任务' }}
                    <span v-if="task.patientId">· {{ task.patientId }}</span>
                  </p>
                </div>
                <div class="queue-item-grade">
                  <span class="med-chip" :class="gradeClass(task.gradingLabel)">{{ task.gradingLabel || '--' }}</span>
                </div>
              </div>

              <div class="queue-item-grid">
                <div>
                  <span class="queue-label">病例号</span>
                  <div class="queue-value med-mono">{{ task.caseNo || '--' }}</div>
                </div>
                <div>
                  <span class="queue-label">模型</span>
                  <div class="queue-value">{{ task.modelVersion || 'caries-v1' }}</div>
                </div>
                <div>
                  <span class="queue-label">创建时间</span>
                  <div class="queue-value med-mono">{{ formatDateTime(task.createdAt) }}</div>
                </div>
                <div>
                  <span class="queue-label">推理耗时</span>
                  <div class="queue-value med-mono">{{ formatDuration(task.inferenceMillis) }}</div>
                </div>
              </div>

              <div class="queue-progress-row">
                <div>
                  <span class="queue-label">不确定性</span>
                  <div class="queue-uncertainty">{{ formatUncertainty(task.uncertaintyScore) }}</div>
                </div>
                <div class="queue-progress">
                  <div class="med-progress"><span :style="{ width: `${Math.min(100, Math.max(6, uncertaintyPercent(task.uncertaintyScore)))}%`, background: uncertaintyColor(task.uncertaintyScore) }"></span></div>
                </div>
              </div>

              <div class="queue-actions">
                <button class="med-btn med-btn--tiny med-btn--ghost" @click.stop="openDetail(task.taskId)">
                  <AppIcon name="scan" :size="13" />
                  详情
                </button>
                <button
                  class="med-btn med-btn--tiny"
                  :class="task.needsReview || normalizeStatus(task.taskStatusCode) === 'REVIEW' ? 'med-btn--primary' : 'med-btn--ghost'"
                  @click.stop="openReview(task.taskId)"
                >
                  <AppIcon name="check" :size="13" />
                  复核
                </button>
                <button class="med-btn med-btn--tiny med-btn--ghost" @click.stop="openReport(task.taskId)">
                  <AppIcon name="report" :size="13" />
                  报告
                </button>
              </div>
            </article>
          </div>

          <div class="queue-footer">
            <span class="med-meta">Showing {{ displayStart }} - {{ displayEnd }} of {{ total }}</span>
            <div class="med-action-row">
              <button class="med-btn med-btn--tiny med-btn--ghost" :disabled="pageNo === 1" @click="prevPage">上一页</button>
              <button class="med-btn med-btn--tiny med-btn--ghost" :disabled="pageNo >= totalPages" @click="nextPage">下一页</button>
            </div>
          </div>
        </div>
      </article>

      <article class="med-card">
        <div class="med-card-inner">
          <div class="med-section-head">
            <h2 class="med-section-title">运行态摘要</h2>
            <span class="med-chip med-chip--accent">LIVE</span>
          </div>

          <div class="queue-side-stack">
            <div class="queue-highlight">
              <div class="queue-highlight-cap">优先处理</div>
              <div v-if="priorityTask" class="queue-highlight-body">
                <div class="queue-highlight-title">{{ priorityTask.taskNo }}</div>
                <div class="queue-highlight-sub">{{ priorityTask.caseNo || priorityTask.patientName || '待补充病例信息' }}</div>
                <div class="queue-highlight-tags">
                  <span class="med-chip" :class="gradeClass(priorityTask.gradingLabel)">{{ priorityTask.gradingLabel || '--' }}</span>
                  <span class="med-chip" :class="statusClass(priorityTask.taskStatusCode)">{{ statusLabel(priorityTask.taskStatusCode) }}</span>
                </div>
                <button class="med-btn med-btn--primary" @click="quickOpen(priorityTask)">
                  <AppIcon :name="priorityTask.needsReview ? 'check' : 'scan'" :size="13" />
                  {{ priorityTask.needsReview ? '进入复核' : '查看详情' }}
                </button>
              </div>
              <div v-else class="med-empty">当前没有高优先级任务。</div>
            </div>

            <div class="med-card queue-side-card">
              <div class="med-card-inner">
                <div class="med-section-head compact">
                  <h3 class="med-section-title">状态分布</h3>
                </div>
                <div class="queue-status-list">
                  <div v-for="item in statusSummary" :key="item.key" class="queue-status-row">
                    <div class="queue-status-label">
                      <span class="queue-status-dot" :style="{ background: item.color }"></span>
                      <span>{{ item.label }}</span>
                    </div>
                    <strong>{{ item.count }}</strong>
                  </div>
                </div>
              </div>
            </div>

            <div class="med-card queue-side-card">
              <div class="med-card-inner">
                <div class="med-section-head compact">
                  <h3 class="med-section-title">自动刷新策略</h3>
                </div>
                <div class="med-note">
                  当前使用设置中心中的刷新频率。你可以在系统设置中修改自动刷新间隔和队列展示偏好。
                </div>
                <button class="med-btn med-btn--ghost queue-settings-btn" @click="router.push('/settings')">
                  <AppIcon name="settings" :size="14" />
                  前往系统设置
                </button>
              </div>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { analysisApi } from '@/api/analysis'
import { loadWorkspaceSettings, onWorkspaceSettingsChange, type WorkspaceSettings } from '@/utils/workbenchSettings'

type RawTask = {
  taskId: number
  taskNo?: string
  caseNo?: string
  patientName?: string
  patientId?: string
  taskTypeCode?: string
  modelVersion?: string
  gradingLabel?: string
  uncertaintyScore?: number
  taskStatusCode?: string
  createdAt?: string
  inferenceMillis?: number
  needsReview?: boolean
}

type StatusFilter = 'ALL' | 'SUCCESS' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'PENDING'

const router = useRouter()
const tasks = ref<RawTask[]>([])
const loading = ref(true)
const pageNo = ref(1)
const total = ref(0)
const totalPages = ref(1)
const keyword = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const settings = ref<WorkspaceSettings>(loadWorkspaceSettings())
let refreshTimer: number | undefined
let cleanupSettingsListener: () => void = () => {}

const normalizeStatus = (status?: string) => String(status || 'PENDING').toUpperCase()

const statusLabel = (status?: string) => {
  switch (normalizeStatus(status)) {
    case 'SUCCESS':
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

const statusClass = (status?: string) => {
  switch (normalizeStatus(status)) {
    case 'SUCCESS':
      return 'med-chip--ok'
    case 'RUNNING':
      return 'med-chip--accent'
    case 'REVIEW':
      return 'med-chip--warn'
    case 'FAILED':
      return 'med-chip--danger'
    default:
      return ''
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

const uncertaintyPercent = (value?: number) => Math.round(Number(value || 0) * 100)
const formatUncertainty = (value?: number) => `${Number(value || 0).toFixed(2)}`
const formatDuration = (value?: number) => (value ? `${(value / 1000).toFixed(2)}s` : '--')

const uncertaintyColor = (value?: number) => {
  const normalized = Number(value || 0)
  if (normalized >= settings.value.riskAlertThreshold) return 'linear-gradient(90deg, #ff636e, #f7a23a)'
  if (normalized >= settings.value.riskAlertThreshold * 0.7) return 'linear-gradient(90deg, #f7a23a, #f7d63a)'
  return 'linear-gradient(90deg, #2ee6c8, #5eead4)'
}

const filteredTasks = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const status = normalizeStatus(task.taskStatusCode)
    const matchesStatus = statusFilter.value === 'ALL' || status === statusFilter.value
    if (!matchesStatus) return false
    if (!q) return true
    return [task.taskNo, task.taskTypeCode, task.modelVersion, task.caseNo, task.patientName, task.patientId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(q))
  })
})

const displayedTasks = computed(() => filteredTasks.value)
const reviewCount = computed(() => tasks.value.filter((task) => normalizeStatus(task.taskStatusCode) === 'REVIEW').length)
const runningCount = computed(() => tasks.value.filter((task) => normalizeStatus(task.taskStatusCode) === 'RUNNING').length)
const averageLatency = computed(() => {
  const values = tasks.value.map((task) => Number(task.inferenceMillis || 0)).filter((value) => value > 0)
  if (!values.length) return '--'
  const avg = values.reduce((sum, value) => sum + value, 0) / values.length
  return `${(avg / 1000).toFixed(2)}s`
})

const tabs = computed(() => {
  const count = (status: StatusFilter) => (status === 'ALL' ? tasks.value.length : tasks.value.filter((task) => normalizeStatus(task.taskStatusCode) === status).length)
  return [
    { code: 'ALL' as const, label: '全部', count: count('ALL') },
    { code: 'SUCCESS' as const, label: '完成', count: count('SUCCESS') },
    { code: 'RUNNING' as const, label: '运行中', count: count('RUNNING') },
    { code: 'REVIEW' as const, label: '复核', count: count('REVIEW') },
    { code: 'FAILED' as const, label: '失败', count: count('FAILED') },
    { code: 'PENDING' as const, label: '排队中', count: count('PENDING') },
  ]
})

const autoRefreshLabel = computed(() => (settings.value.autoRefreshSeconds > 0 ? `${settings.value.autoRefreshSeconds}s` : 'OFF'))
const displayStart = computed(() => (total.value === 0 ? 0 : (pageNo.value - 1) * 10 + 1))
const displayEnd = computed(() => Math.min(pageNo.value * 10, total.value))

const statusSummary = computed(() => [
  { key: 'review', label: '待复核', count: reviewCount.value, color: '#f7a23a' },
  { key: 'running', label: '运行中', count: runningCount.value, color: '#5eead4' },
  { key: 'done', label: '已完成', count: tasks.value.filter((task) => normalizeStatus(task.taskStatusCode) === 'SUCCESS').length, color: '#2ee6c8' },
  { key: 'failed', label: '失败', count: tasks.value.filter((task) => normalizeStatus(task.taskStatusCode) === 'FAILED').length, color: '#ff636e' },
])

const priorityTask = computed(() => {
  const priorityOrder = ['REVIEW', 'RUNNING', 'FAILED', 'SUCCESS', 'PENDING']
  return [...tasks.value].sort((a, b) => {
    const statusDiff = priorityOrder.indexOf(normalizeStatus(a.taskStatusCode)) - priorityOrder.indexOf(normalizeStatus(b.taskStatusCode))
    if (statusDiff !== 0) return statusDiff
    return uncertaintyPercent(b.uncertaintyScore) - uncertaintyPercent(a.uncertaintyScore)
  })[0]
})

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString()
}

const fetchTasks = async () => {
  loading.value = true
  try {
    const res = await analysisApi.getTasks({ pageNo: pageNo.value, pageSize: 10 })
    tasks.value = (res.data.records || res.data.list || []) as RawTask[]
    total.value = Number(res.data.total || 0)
    totalPages.value = Math.max(1, Math.ceil(total.value / 10))
  } catch (error) {
    console.error('Failed to fetch tasks', error)
    tasks.value = []
    total.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const clearTimer = () => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

const setupAutoRefresh = () => {
  clearTimer()
  if (settings.value.autoRefreshSeconds <= 0) return
  refreshTimer = window.setInterval(() => {
    void fetchTasks()
  }, settings.value.autoRefreshSeconds * 1000)
}

const reload = () => {
  void fetchTasks()
}

const nextPage = () => {
  if (pageNo.value >= totalPages.value) return
  pageNo.value += 1
  void fetchTasks()
}

const prevPage = () => {
  if (pageNo.value <= 1) return
  pageNo.value -= 1
  void fetchTasks()
}

const openDetail = (taskId: number) => router.push(`/analysis/${taskId}`)
const openReview = (taskId: number) => router.push(`/review/${taskId}`)
const openReport = (taskId: number) => router.push(`/reports/${taskId}`)
const quickOpen = (task: RawTask) => {
  if (task.needsReview || normalizeStatus(task.taskStatusCode) === 'REVIEW') openReview(task.taskId)
  else openDetail(task.taskId)
}

onMounted(() => {
  void fetchTasks()
  setupAutoRefresh()
  cleanupSettingsListener = onWorkspaceSettingsChange((nextSettings) => {
    settings.value = nextSettings
    setupAutoRefresh()
  })
})

onUnmounted(() => {
  clearTimer()
  cleanupSettingsListener()
})
</script>

<style scoped>
.queue-page {
  gap: 16px;
}

.queue-filter-shell {
  display: grid;
  gap: 14px;
}

.queue-search {
  max-width: 420px;
}

.queue-body {
  align-items: start;
}

.queue-list {
  display: grid;
  gap: 12px;
}

.queue-item {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(94, 234, 212, 0.08);
  background: rgba(11, 36, 44, 0.42);
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, background .18s ease;
}

.queue-item:hover {
  transform: translateY(-2px);
  border-color: rgba(94, 234, 212, 0.2);
  background: rgba(11, 36, 44, 0.58);
}

.queue-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.queue-item-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--text);
  font-size: 15px;
  font-weight: 700;
}

.queue-item-sub {
  margin: 8px 0 0;
  color: var(--text-soft);
  font-size: 12px;
}

.queue-item-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-dim);
  text-transform: uppercase;
}

.queue-value {
  margin-top: 6px;
  color: var(--text);
  font-size: 13px;
}

.queue-progress-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}

.queue-uncertainty {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text);
  font-family: Consolas, 'JetBrains Mono', monospace;
}

.queue-actions,
.queue-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.queue-actions {
  margin-top: 16px;
}

.queue-footer {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(94, 234, 212, 0.08);
}

.queue-side-stack {
  display: grid;
  gap: 14px;
}

.queue-highlight {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(46, 230, 200, 0.16);
  background: radial-gradient(circle at top right, rgba(46, 230, 200, 0.18), transparent 46%), rgba(11, 36, 44, 0.52);
}

.queue-highlight-cap {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-dim);
}

.queue-highlight-title {
  margin-top: 12px;
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
}

.queue-highlight-sub {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-soft);
}

.queue-highlight-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 16px 0;
}

.queue-status-list {
  display: grid;
  gap: 12px;
}

.queue-status-row,
.queue-status-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.queue-status-label {
  justify-content: flex-start;
  color: var(--text-soft);
}

.queue-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  box-shadow: 0 0 10px currentColor;
}

.queue-settings-btn {
  width: 100%;
  margin-top: 14px;
}

.compact {
  margin-bottom: 0;
}

@media (max-width: 1180px) {
  .queue-body {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 760px) {
  .queue-item-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .queue-progress-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
