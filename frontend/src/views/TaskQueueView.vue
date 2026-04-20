<template>
  <div class="page">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">AI Analysis</div>
        <h1 class="page-hello-title">分析任务队列</h1>
      </div>
      <button class="btn btn-primary" @click="reload">
        <AppIcon name="scan" :size="14" />
        刷新队列
      </button>
    </div>

    <div class="card" style="padding: 16px; margin-bottom: 16px">
      <div style="display: flex; gap: 12px; justify-content: space-between; align-items: center; flex-wrap: wrap">
        <div class="lib-search" style="width: 360px">
          <AppIcon name="search" :size="15" />
          <input v-model.trim="keyword" type="text" placeholder="搜索任务号、任务类型、模型版本" />
        </div>
        <div class="lib-filter-group">
          <button
            v-for="item in tabs"
            :key="item.code"
            class="lib-filter"
            :class="{ on: statusFilter === item.code }"
            @click="statusFilter = item.code"
          >
            <span>{{ item.label }}</span>
            <span class="mono">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </div>

    <section class="card" style="overflow: hidden">
      <div class="card-head">
        <h3>任务列表</h3>
        <div class="card-head-actions">
          <span class="micro">Page {{ pageNo }} / {{ totalPages }}</span>
        </div>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>任务号</th>
            <th>任务摘要</th>
            <th>分级</th>
            <th>不确定性</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>耗时</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" style="text-align: center; color: var(--ink-3)">正在加载分析任务...</td>
          </tr>
          <tr v-else-if="displayedTasks.length === 0">
            <td colspan="8" style="text-align: center; color: var(--ink-3)">当前筛选条件下没有任务。</td>
          </tr>
          <tr v-for="task in displayedTasks" :key="task.taskId" @click="openDetail(task.taskId)">
            <td>
              <div style="display: flex; flex-direction: column; gap: 2px">
                <span class="mono" style="color: var(--brand-700)">{{ task.taskNo || `TASK-${task.taskId}` }}</span>
                <span class="micro" style="letter-spacing: .04em">{{ task.caseNo || 'AI task' }}</span>
              </div>
            </td>
            <td>
              <div style="display: flex; flex-direction: column; gap: 2px">
                <span>{{ task.taskTypeCode || 'Inference' }}</span>
                <span class="dt-sub">{{ task.modelVersion || 'Unknown model' }}</span>
              </div>
            </td>
            <td>
              <span class="chip" :style="gradeChipStyle(task.gradingLabel)">
                {{ task.gradingLabel || '--' }}
              </span>
            </td>
            <td>
              <div style="display: flex; align-items: center; gap: 10px; min-width: 140px">
                <div style="flex: 1; height: 8px; background: var(--surface-sunk); border-radius: 999px; overflow: hidden">
                  <div
                    :style="{
                      width: `${Math.min(100, Math.max(0, Number(task.uncertaintyScore || 0) * 100))}%`,
                      height: '100%',
                      background: Number(task.uncertaintyScore || 0) >= 0.35 ? 'var(--warn-500)' : 'var(--brand-500)'
                    }"
                  ></div>
                </div>
                <span class="mono" style="font-size: 11px; color: var(--ink-3)">
                  {{ Number(task.uncertaintyScore || 0).toFixed(2) }}
                </span>
              </div>
            </td>
            <td>
              <span class="chip" :style="statusChipStyle(task.taskStatusCode)">
                {{ statusLabel(task.taskStatusCode) }}
              </span>
            </td>
            <td>
              <span class="mono" style="font-size: 11px; color: var(--ink-3)">
                {{ formatDateTime(task.createdAt) }}
              </span>
            </td>
            <td>
              <span class="mono" style="font-size: 11px; color: var(--ink-3)">
                {{ task.inferenceMillis ? `${(task.inferenceMillis / 1000).toFixed(1)}s` : '--' }}
              </span>
            </td>
            <td>
              <button class="btn btn-subtle btn-sm" @click.stop="openDetail(task.taskId)">
                查看
                <AppIcon name="chevron_right" :size="14" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div style="display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-top: 1px solid var(--line)">
        <span class="micro">Showing {{ displayStart }} - {{ displayEnd }} of {{ total }}</span>
        <div style="display: flex; gap: 8px">
          <button class="btn btn-ghost btn-sm" :disabled="pageNo === 1" @click="prevPage">上一页</button>
          <button class="btn btn-ghost btn-sm" :disabled="pageNo >= totalPages" @click="nextPage">下一页</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { analysisApi } from '@/api/analysis'

type RawTask = {
  taskId: number
  taskNo?: string
  caseNo?: string
  taskTypeCode?: string
  modelVersion?: string
  gradingLabel?: string
  uncertaintyScore?: number
  taskStatusCode?: string
  createdAt?: string
  inferenceMillis?: number
}

const router = useRouter()

const tasks = ref<RawTask[]>([])
const loading = ref(true)
const pageNo = ref(1)
const total = ref(0)
const totalPages = ref(1)
const keyword = ref('')
const statusFilter = ref<'ALL' | 'SUCCESS' | 'RUNNING' | 'REVIEW' | 'FAILED'>('ALL')

const statusLabel = (status?: string) => {
  switch ((status || '').toUpperCase()) {
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

const statusChipStyle = (status?: string) => {
  switch ((status || '').toUpperCase()) {
    case 'SUCCESS':
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

const filteredTasks = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const matchStatus = statusFilter.value === 'ALL' || (task.taskStatusCode || '').toUpperCase() === statusFilter.value
    if (!matchStatus) return false
    if (!q) return true
    return [task.taskNo, task.taskTypeCode, task.modelVersion, task.caseNo]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(q))
  })
})

const displayedTasks = computed(() => filteredTasks.value)

const tabs = computed(() => {
  const count = (status?: string) =>
    tasks.value.filter((task) => ((task.taskStatusCode || '').toUpperCase() || 'QUEUED') === status).length

  return [
    { code: 'ALL' as const, label: '全部', count: tasks.value.length },
    { code: 'SUCCESS' as const, label: '完成', count: count('SUCCESS') },
    { code: 'RUNNING' as const, label: '运行中', count: count('RUNNING') },
    { code: 'REVIEW' as const, label: '复核', count: count('REVIEW') },
    { code: 'FAILED' as const, label: '失败', count: count('FAILED') }
  ]
})

const displayStart = computed(() => (total.value === 0 ? 0 : (pageNo.value - 1) * 10 + 1))
const displayEnd = computed(() => Math.min(pageNo.value * 10, total.value))

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

const openDetail = (taskId: number) => {
  router.push(`/analysis/${taskId}`)
}

onMounted(() => {
  void fetchTasks()
})
</script>
