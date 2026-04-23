<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import AppIcon from '@/components/AppIcon.vue'
import { dashboardApi } from '@/api/dashboard'
import { analysisApi } from '@/api/analysis'

interface DashboardData {
  kpi?: {
    totalScans?: number
    agreementRate?: number
    reviewRate?: number
    avgLatencyMs?: number
  }
  events?: Array<{
    eventId?: string
    type?: string
    message?: string
    timestamp?: string
  }>
}

interface CaseItem {
  id: number
  no: string
  subtitle: string
  time: string
  findings: string
  severity: string
  status: 'analyzing' | 'done' | 'flagged'
}

const router = useRouter()
const { locale } = useI18n()

const dashboard = ref<DashboardData | null>(null)
const cases = ref<CaseItem[]>([])

const sparkData = (values: number[], color: string, height = 40) => {
  const width = 140
  const max = Math.max(...values)
  const min = Math.min(...values)
  const pts = values
    .map((value, index) => {
      const x = (index / Math.max(values.length - 1, 1)) * width
      const y = height - ((value - min) / Math.max(max - min, 1)) * (height - 4) - 2
      return `${x},${y}`
    })
    .join(' ')
  return {
    w: width,
    h: height,
    pts,
    color,
    areaPts: `0,${height} ${pts} ${width},${height}`
  }
}

const stats = computed(() => {
  const kpi = dashboard.value?.kpi
  return [
    {
      key: locale.value === 'zh' ? '总扫描数' : 'Total scans',
      value: String(kpi?.totalScans ?? '--'),
      delta: '+0',
      spark: sparkData([40, 44, 48, 56, 62, 70, 78], 'var(--brand-500)')
    },
    {
      key: locale.value === 'zh' ? '一致率' : 'Agreement rate',
      value: kpi?.agreementRate != null ? `${(kpi.agreementRate * 100).toFixed(1)}%` : '--',
      delta: '+0.8',
      spark: sparkData([92, 93, 93, 94, 94, 95, 95], 'var(--ok-500)')
    },
    {
      key: locale.value === 'zh' ? '复核占比' : 'Review rate',
      value: kpi?.reviewRate != null ? `${(kpi.reviewRate * 100).toFixed(1)}%` : '--',
      delta: '-1.2',
      spark: sparkData([8, 8, 7, 6, 6, 5, 5], 'var(--warn-500)')
    },
    {
      key: locale.value === 'zh' ? '平均时延' : 'Avg latency',
      value: kpi?.avgLatencyMs != null ? `${(kpi.avgLatencyMs / 1000).toFixed(2)}s` : '--',
      delta: '-0.2',
      spark: sparkData([1.2, 1.15, 1.08, 1.02, 0.98, 0.9, 0.84], 'var(--info-500)')
    }
  ]
})

const recentEvents = computed(() => {
  if (dashboard.value?.events?.length) return dashboard.value.events.slice(0, 4)
  return [
    { eventId: 'fallback-1', type: 'SUCCESS', message: '已签发 3 份报告', timestamp: new Date().toISOString() },
    { eventId: 'fallback-2', type: 'WARNING', message: '1 条高不确定任务进入复核', timestamp: new Date().toISOString() }
  ]
})

const statusText = (status: CaseItem['status']) => {
  if (status === 'analyzing') return locale.value === 'zh' ? '分析中' : 'Analyzing'
  if (status === 'flagged') return locale.value === 'zh' ? '需复核' : 'Review'
  return locale.value === 'zh' ? '已完成' : 'Done'
}

const statusClass = (status: CaseItem['status']) => {
  if (status === 'analyzing') return 'status status-analyzing'
  if (status === 'flagged') return 'status status-flag'
  return 'status status-done'
}

const loadDashboard = async () => {
  try {
    const [dashboardRes, taskRes] = await Promise.all([
      dashboardApi.getNeuralDashboard(),
      analysisApi.getTasks({ pageNo: 1, pageSize: 5 })
    ])

    dashboard.value = dashboardRes.data as DashboardData

    const records = (taskRes.data.records || taskRes.data.list || []) as Array<{
      taskId: number
      taskNo?: string
      taskStatusCode?: string
      createdAt?: string
      inferenceMillis?: number
    }>

    cases.value = records.map((item) => {
      const rawStatus = String(item.taskStatusCode || '').toUpperCase()
      const status: CaseItem['status'] =
        rawStatus === 'RUNNING'
          ? 'analyzing'
          : rawStatus === 'REVIEW'
            ? 'flagged'
            : 'done'

      return {
        id: Number(item.taskId),
        no: item.taskNo || `TASK-${item.taskId}`,
        subtitle: rawStatus || 'QUEUED',
        time: item.createdAt ? new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--',
        findings: item.inferenceMillis ? `${(item.inferenceMillis / 1000).toFixed(1)}s` : '--',
        severity: status === 'flagged' ? 'Needs review' : status === 'analyzing' ? 'Running' : 'Completed',
        status
      }
    })
  } catch (error) {
    console.error('Failed to load dashboard', error)
  }
}

const openAnalysis = (taskId?: number) => {
  if (taskId) {
    router.push(`/analysis/${taskId}`)
    return
  }
  router.push('/analysis')
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="page page-home">
    <div class="page-hello">
      <div>
        <div class="micro">{{ locale === 'zh' ? '欢迎回来 · 今日工作概览' : 'Welcome back · Today overview' }}</div>
        <h1 class="page-hello-title">
          {{ locale === 'zh' ? '当前 Java BFF 侧分析、复核与报告流已接入新布局。' : 'The Java BFF analysis, review, and reporting flows are now mounted on the new layout.' }}
        </h1>
      </div>
      <NButton type="primary" size="large" @click="openAnalysis()">
        <template #icon><AppIcon name="upload" :size="15" /></template>
        {{ locale === 'zh' ? '上传新影像' : 'Upload new image' }}
      </NButton>
    </div>

    <div class="home-stats">
      <div v-for="(stat, index) in stats" :key="index" class="card home-stat">
        <div class="home-stat-head">
          <span class="micro">{{ stat.key }}</span>
          <span class="home-stat-delta" :style="{ color: stat.delta.startsWith('+') ? 'var(--ok-700)' : 'var(--danger-700)' }">
            {{ stat.delta }}
          </span>
        </div>
        <div class="home-stat-row">
          <div class="home-stat-n">{{ stat.value }}</div>
          <svg :width="stat.spark.w" :height="stat.spark.h" :viewBox="`0 0 ${stat.spark.w} ${stat.spark.h}`">
            <polygon :points="stat.spark.areaPts" :fill="stat.spark.color" opacity=".1" />
            <polyline :points="stat.spark.pts" fill="none" :stroke="stat.spark.color" stroke-width="1.5" stroke-linejoin="round" />
          </svg>
        </div>
      </div>
    </div>

    <div class="home-grid">
      <section class="card home-queue">
        <div class="card-head">
          <h3>{{ locale === 'zh' ? '最近分析任务' : 'Recent analysis tasks' }}</h3>
          <div class="card-head-actions">
            <button class="chip chip-neutral">{{ locale === 'zh' ? '分析' : 'Analysis' }}</button>
            <button class="chip">{{ locale === 'zh' ? 'Java BFF' : 'Java BFF' }}</button>
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ locale === 'zh' ? '任务号' : 'Task' }}</th>
              <th>{{ locale === 'zh' ? '摘要' : 'Summary' }}</th>
              <th>{{ locale === 'zh' ? '时间' : 'Time' }}</th>
              <th>{{ locale === 'zh' ? '耗时' : 'Duration' }}</th>
              <th>{{ locale === 'zh' ? '状态' : 'Status' }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in cases" :key="item.id" @click="openAnalysis(item.id)">
              <td><span class="mono" style="color: var(--ink-3)">{{ item.no }}</span></td>
              <td>
                <div class="dt-patient">
                  <div class="dt-avatar">{{ item.no.slice(-1) }}</div>
                  <div>
                    <div>{{ item.severity }}</div>
                    <div class="dt-sub">{{ item.subtitle }}</div>
                  </div>
                </div>
              </td>
              <td><span class="mono" style="font-size: 12px; color: var(--ink-3)">{{ item.time }}</span></td>
              <td><span class="mono">{{ item.findings }}</span></td>
              <td>
                <span :class="statusClass(item.status)">
                  <span v-if="item.status === 'analyzing'" class="dot"></span>
                  <AppIcon v-else-if="item.status === 'done'" name="check" :size="11" />
                  <AppIcon v-else name="alert" :size="11" />
                  {{ statusText(item.status) }}
                </span>
              </td>
              <td><AppIcon name="chevron_right" :size="16" style="color: var(--ink-4)" /></td>
            </tr>
          </tbody>
        </table>
      </section>

      <aside class="home-side">
        <section class="card home-upload">
          <div class="card-head"><h3>{{ locale === 'zh' ? '快速入口' : 'Quick access' }}</h3></div>
          <button class="home-drop" @click="openAnalysis()">
            <AppIcon name="upload" :size="26" />
            <div class="home-drop-t">{{ locale === 'zh' ? '进入分析队列' : 'Open analysis queue' }}</div>
            <div class="home-drop-s">{{ locale === 'zh' ? '继续沿用 Java -> RabbitMQ -> Python 主链路' : 'Keep the Java -> RabbitMQ -> Python mainline' }}</div>
          </button>
        </section>

        <section class="card home-recent">
          <div class="card-head"><h3>{{ locale === 'zh' ? '近期事件' : 'Recent events' }}</h3></div>
          <ul class="home-timeline">
            <li v-for="event in recentEvents" :key="event.eventId || event.message">
              <div
                class="ht-dot"
                :style="{
                  background:
                    event.type === 'ERROR'
                      ? 'var(--danger-500)'
                      : event.type === 'WARNING'
                        ? 'var(--warn-500)'
                        : event.type === 'SUCCESS'
                          ? 'var(--ok-500)'
                          : 'var(--info-500)'
                }"
              ></div>
              <div>
                <div>{{ event.message || '--' }}</div>
                <div class="dt-sub mono">{{ event.timestamp ? new Date(event.timestamp).toLocaleString() : '--' }}</div>
              </div>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>
