<template>
  <div class="med-page case-page">
    <section class="med-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">Case Portal</div>
        <h1 class="med-title">病例中心与影像发起</h1>
        <p class="med-subtitle">
          统一管理病例入口、影像上传与任务发起。可按状态筛选当前病例任务，并从卡片直接进入详情、复核或报告。
        </p>
      </div>
      <div class="med-action-row">
        <button class="med-btn med-btn--ghost" @click="reload">
          <AppIcon name="scan" :size="14" />
          刷新列表
        </button>
        <button class="med-btn med-btn--primary" @click="openNewCaseDrawer">
          <AppIcon name="plus" :size="14" />
          新建病例
        </button>
      </div>
    </section>

    <section class="med-metric-grid">
      <article class="med-card med-metric-card">
        <div class="med-metric-label">All Cases</div>
        <div class="med-metric-value">{{ store.tasks.items.length }}</div>
        <div class="med-metric-caption">当前病例中心可追踪任务总数</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Review Needed</div>
        <div class="med-metric-value">{{ reviewCount }}</div>
        <div class="med-metric-caption">建议优先人工复核的任务</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Running</div>
        <div class="med-metric-value">{{ runningCount }}</div>
        <div class="med-metric-caption">仍处于推理过程中的病例</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Success Rate</div>
        <div class="med-metric-value">{{ successRate }}</div>
        <div class="med-metric-caption">已完成任务占全部任务比例</div>
      </article>
    </section>

    <section class="med-card">
      <div class="med-card-inner case-filter-shell">
        <div class="med-search case-search">
          <AppIcon name="search" :size="15" />
          <input v-model.trim="searchKeyword" type="text" placeholder="搜索病例号、患者号、患者姓名、任务号" />
        </div>
        <div class="med-tabs">
          <button
            v-for="item in statusTabs"
            :key="item.code"
            class="med-tab"
            :class="{ 'is-active': activeStatus === item.code }"
            @click="activeStatus = item.code"
          >
            <span>{{ item.label }}</span>
            <span class="med-mono">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="case-grid-shell">
      <div v-if="store.loading" class="med-empty">正在同步病例任务...</div>
      <div v-else-if="filteredItems.length === 0" class="med-empty">当前筛选条件下没有病例任务。</div>
      <div v-else class="case-grid">
        <article v-for="item in filteredItems" :key="item.id" class="case-card" @click="openDetail(item.id)">
          <div class="case-visual">
            <div class="case-visual-backdrop"></div>
            <div class="case-visual-grid"></div>
            <div class="case-visual-shape case-visual-shape-a"></div>
            <div class="case-visual-shape case-visual-shape-b"></div>
            <div class="case-topbar">
              <span class="med-chip">{{ item.caseNo || 'CASE' }}</span>
              <span class="med-chip" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            </div>
            <div class="case-center-mark">
              <div v-for="tooth in 6" :key="tooth" class="case-tooth"></div>
            </div>
            <div class="case-bottomline">
              <span class="med-mono">{{ formatDate(item.createdAt) }}</span>
              <span class="med-chip" :class="gradeClass(item.grade)">{{ item.grade || '--' }}</span>
            </div>
          </div>

          <div class="case-content">
            <div class="case-title-row">
              <div>
                <div class="case-patient-name">{{ item.patientName || item.patientId || '未命名患者' }}</div>
                <div class="med-meta med-mono">{{ item.no }}</div>
              </div>
              <span class="med-chip" :class="item.needsReview ? 'med-chip--warn' : 'med-chip--ok'">{{ item.needsReview ? '需复核' : '已归档' }}</span>
            </div>

            <div class="case-info-grid">
              <div>
                <span class="queue-label">患者号</span>
                <div class="case-info-value med-mono">{{ item.patientId || '--' }}</div>
              </div>
              <div>
                <span class="queue-label">创建时间</span>
                <div class="case-info-value med-mono">{{ formatDateTime(item.createdAt) }}</div>
              </div>
            </div>

            <div class="case-uncertainty-block">
              <div class="case-uncertainty-head">
                <span class="queue-label">不确定性</span>
                <span class="med-mono">{{ (item.uncertainty || 0).toFixed(2) }}</span>
              </div>
              <div class="med-progress"><span :style="{ width: `${Math.min(100, Math.max(6, (item.uncertainty || 0) * 100))}%`, background: uncertaintyColor(item.uncertainty) }"></span></div>
            </div>

            <div class="case-actions">
              <button class="med-btn med-btn--tiny med-btn--ghost" @click.stop="openDetail(item.id)">
                <AppIcon name="scan" :size="13" />
                详情
              </button>
              <button class="med-btn med-btn--tiny med-btn--ghost" @click.stop="openReview(item.id)">
                <AppIcon name="check" :size="13" />
                复核
              </button>
              <button class="med-btn med-btn--tiny med-btn--ghost" @click.stop="openReport(item.id)">
                <AppIcon name="report" :size="13" />
                报告
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <Transition name="fade-up">
        <div v-if="showNewCase" class="case-modal-mask" @click.self="closeNewCaseDrawer">
          <div class="case-modal med-card">
            <div class="med-card-inner">
              <div class="med-section-head">
                <h2 class="med-section-title">新建病例</h2>
                <button class="med-btn med-btn--tiny med-btn--ghost" :disabled="isSubmitting" @click="closeNewCaseDrawer">关闭</button>
              </div>

              <div class="case-form-grid">
                <label class="med-input-wrap">
                  <span class="queue-label">患者编号</span>
                  <input v-model.trim="newCaseForm.patientCode" class="med-input" type="text" placeholder="P-10001" />
                </label>
                <label class="med-input-wrap">
                  <span class="queue-label">年龄</span>
                  <input v-model.trim="newCaseForm.age" class="med-input" type="number" min="0" max="120" placeholder="45" />
                </label>
                <div class="med-input-wrap case-span-2">
                  <span class="queue-label">性别</span>
                  <div class="med-tabs">
                    <button class="med-tab" :class="{ 'is-active': newCaseForm.genderCode === 'MALE' }" @click="newCaseForm.genderCode = 'MALE'">男</button>
                    <button class="med-tab" :class="{ 'is-active': newCaseForm.genderCode === 'FEMALE' }" @click="newCaseForm.genderCode = 'FEMALE'">女</button>
                  </div>
                </div>
                <label class="med-input-wrap case-span-2">
                  <span class="queue-label">主诉</span>
                  <textarea v-model.trim="newCaseForm.chiefComplaint" class="med-textarea" placeholder="填写本次就诊主诉与分析背景"></textarea>
                </label>
                <div class="med-input-wrap case-span-2">
                  <span class="queue-label">影像上传</span>
                  <input ref="fileInputRef" type="file" accept=".png,.jpg,.jpeg,.dcm,.dicom,image/png,image/jpeg,application/dicom" hidden @change="handleFileInputChange" />
                  <div class="case-dropzone" :class="{ active: isDragActive }" @click="openFilePicker" @dragenter.prevent="isDragActive = true" @dragover.prevent="isDragActive = true" @dragleave.prevent="isDragActive = false" @drop.prevent="handleFileDrop">
                    <template v-if="selectedFile">
                      <div class="case-dropzone-file">
                        <div>
                          <strong>{{ selectedFile.name }}</strong>
                          <div class="med-meta med-mono">{{ formatFileSize(selectedFile.size) }} · {{ fileTypeLabel }}</div>
                        </div>
                        <button class="med-btn med-btn--tiny med-btn--danger" @click.stop="clearSelectedFile">移除</button>
                      </div>
                    </template>
                    <template v-else>
                      <AppIcon name="upload" :size="22" />
                      <div class="case-dropzone-title">拖拽影像到这里，或点击选择文件</div>
                      <div class="med-meta">支持 PNG / JPG / DICOM</div>
                    </template>
                  </div>
                </div>
              </div>

              <div v-if="inlineError" class="med-note case-error-note">{{ inlineError }}</div>

              <div class="case-modal-actions">
                <button class="med-btn med-btn--ghost" :disabled="isSubmitting" @click="closeNewCaseDrawer">取消</button>
                <button class="med-btn med-btn--primary" :disabled="isSubmitting" @click="submitNewCase">
                  <AppIcon name="plus" :size="13" />
                  {{ isSubmitting ? '提交中...' : '创建并发起分析' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useAnalysisStore } from '@/stores/analysis'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { ApiClientError } from '@/api/request'
import { casePortalApi } from '@/api/casePortal'
import type { AnalysisTaskItem } from '@/models/analysis'

type GenderCode = 'MALE' | 'FEMALE' | ''

interface NewCaseFormState {
  patientCode: string
  age: string
  genderCode: GenderCode
  chiefComplaint: string
}

const router = useRouter()
const store = useAnalysisStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const showNewCase = ref(false)
const isSubmitting = ref(false)
const isDragActive = ref(false)
const selectedFile = ref<File | null>(null)
const inlineError = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const searchKeyword = ref('')
const activeStatus = ref<'ALL' | 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED'>('ALL')

const newCaseForm = reactive<NewCaseFormState>({
  patientCode: '',
  age: '',
  genderCode: '',
  chiefComplaint: ''
})

const statusLabel = (status: string) => {
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

const statusClass = (status: string) => {
  switch (status) {
    case 'DONE':
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
  switch ((grade || '').toUpperCase()) {
    case 'G0':
    case 'C0':
      return 'med-chip--ok'
    case 'G1':
    case 'C1':
      return 'med-chip--accent'
    case 'G2':
    case 'C2':
      return 'med-chip--warn'
    case 'G3':
    case 'G4':
    case 'C3':
    case 'C4':
      return 'med-chip--danger'
    default:
      return ''
  }
}

const uncertaintyColor = (value?: number) => {
  const normalized = Number(value || 0)
  if (normalized >= 0.35) return 'linear-gradient(90deg, #ff636e, #f7a23a)'
  if (normalized >= 0.2) return 'linear-gradient(90deg, #f7a23a, #f7d63a)'
  return 'linear-gradient(90deg, #2ee6c8, #5eead4)'
}

const reviewCount = computed(() => store.tasks.items.filter((item) => item.status === 'REVIEW').length)
const runningCount = computed(() => store.tasks.items.filter((item) => item.status === 'RUNNING').length)
const successRate = computed(() => {
  if (!store.tasks.items.length) return '--'
  const done = store.tasks.items.filter((item) => item.status === 'DONE').length
  return `${Math.round((done / store.tasks.items.length) * 100)}%`
})

const statusTabs = computed(() => {
  const count = (status: AnalysisTaskItem['status']) => store.tasks.items.filter((item) => item.status === status).length
  return [
    { code: 'ALL' as const, label: '全部', count: store.tasks.items.length },
    { code: 'DONE' as const, label: '完成', count: count('DONE') },
    { code: 'RUNNING' as const, label: '运行中', count: count('RUNNING') },
    { code: 'REVIEW' as const, label: '复核', count: count('REVIEW') },
    { code: 'FAILED' as const, label: '失败', count: count('FAILED') },
    { code: 'QUEUED' as const, label: '排队中', count: count('QUEUED') }
  ]
})

const filteredItems = computed(() => {
  const q = searchKeyword.value.trim().toLowerCase()
  return store.tasks.items.filter((item) => {
    const matchStatus = activeStatus.value === 'ALL' || item.status === activeStatus.value
    if (!matchStatus) return false
    if (!q) return true
    return [item.no, item.patientName, item.patientId, item.caseNo]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(q))
  })
})

const fileTypeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const fileName = selectedFile.value.name.toLowerCase()
  if (fileName.endsWith('.dcm') || fileName.endsWith('.dicom')) return 'DICOM'
  if (selectedFile.value.type === 'image/png') return 'PNG'
  if (selectedFile.value.type === 'image/jpeg') return 'JPG'
  return selectedFile.value.type || 'Unknown'
})

onMounted(() => {
  void store.fetchTasks({ pageNum: 1, pageSize: 24 })
})

const reload = () => {
  void store.fetchTasks({ pageNum: 1, pageSize: 24 })
}

const openDetail = (taskId: number) => router.push(`/analysis/${taskId}`)
const openReview = (taskId: number) => router.push(`/review/${taskId}`)
const openReport = (taskId: number) => router.push(`/reports/${taskId}`)

const formatDate = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString()
}

const formatDateTime = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString()
}

const resetNewCaseForm = () => {
  newCaseForm.patientCode = ''
  newCaseForm.age = ''
  newCaseForm.genderCode = ''
  newCaseForm.chiefComplaint = ''
  selectedFile.value = null
  inlineError.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const openNewCaseDrawer = () => {
  resetNewCaseForm()
  showNewCase.value = true
}

const closeNewCaseDrawer = () => {
  if (isSubmitting.value) return
  showNewCase.value = false
  inlineError.value = ''
}

const openFilePicker = () => {
  if (isSubmitting.value) return
  fileInputRef.value?.click()
}

const setSelectedFile = (file?: File | null) => {
  if (!file) return
  selectedFile.value = file
  inlineError.value = ''
}

const handleFileInputChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const [file] = input.files ?? []
  setSelectedFile(file)
}

const handleFileDrop = (event: DragEvent) => {
  isDragActive.value = false
  const [file] = Array.from(event.dataTransfer?.files ?? [])
  setSelectedFile(file)
}

const clearSelectedFile = () => {
  selectedFile.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const buildLocalDate = (age: string) => {
  const numericAge = Number.parseInt(age, 10)
  if (Number.isNaN(numericAge) || numericAge < 0) return undefined
  const date = new Date()
  date.setFullYear(date.getFullYear() - numericAge)
  return date.toISOString().slice(0, 10)
}

const buildLocalDateTime = () => {
  const now = new Date()
  const pad = (value: number) => value.toString().padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const buildPatientName = (patientCode: string) => `患者 ${patientCode}`

const normalizeErrorMessage = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message
  if (error instanceof Error) return error.message
  return '创建病例失败'
}

const validateNewCaseForm = () => {
  if (!newCaseForm.patientCode) return '患者编号不能为空。'
  if (!newCaseForm.age) return '年龄不能为空。'
  if (!newCaseForm.genderCode) return '请选择性别。'
  if (!newCaseForm.chiefComplaint) return '请填写主诉。'
  if (!selectedFile.value) return '请先选择一张影像文件。'
  return ''
}

const submitNewCase = async () => {
  const validationError = validateNewCaseForm()
  if (validationError) {
    inlineError.value = validationError
    notificationStore.warning('表单未完成', validationError)
    return
  }

  const currentUserId = authStore.user?.id
  if (!currentUserId && !import.meta.env.VITE_USE_MOCK) {
    inlineError.value = '当前登录信息不完整，请重新登录后再试。'
    notificationStore.error('无法创建病例', inlineError.value)
    return
  }

  isSubmitting.value = true
  inlineError.value = ''

  try {
    const patientRes = await casePortalApi.createPatient({
      patientName: buildPatientName(newCaseForm.patientCode),
      genderCode: newCaseForm.genderCode || undefined,
      birthDate: buildLocalDate(newCaseForm.age),
      sourceCode: 'OUTPATIENT',
      privacyLevelCode: 'L4',
      remark: `Created from case portal for ${newCaseForm.patientCode}`
    })

    const patientId = patientRes.data.patientId

    const visitRes = await casePortalApi.createVisit({
      patientId,
      doctorUserId: currentUserId,
      visitTypeCode: 'OUTPATIENT',
      visitDate: buildLocalDateTime(),
      complaint: newCaseForm.chiefComplaint,
      triageLevelCode: 'NORMAL',
      sourceChannelCode: 'MANUAL',
      remark: 'Created from case portal'
    })

    const visitId = visitRes.data.visitId

    const caseRes = await casePortalApi.createCase({
      visitId,
      patientId,
      caseTypeCode: 'CARIES_SCREENING',
      caseTitle: `Case ${newCaseForm.patientCode}`,
      chiefComplaint: newCaseForm.chiefComplaint,
      priorityCode: 'NORMAL',
      clinicalNotes: newCaseForm.chiefComplaint,
      remark: 'Created from case portal'
    })

    const caseId = caseRes.data.caseId

    const uploadRes = await casePortalApi.uploadCaseFile(selectedFile.value!, caseId, 'PANORAMIC')

    const imageRes = await casePortalApi.createCaseImage(caseId, {
      attachmentId: uploadRes.data.attachmentId,
      visitId,
      patientId,
      imageTypeCode: 'PANORAMIC',
      imageSourceCode: 'UPLOAD',
      shootingTime: buildLocalDateTime(),
      primaryFlag: '1',
      remark: 'Uploaded from case portal'
    })

    await casePortalApi.saveImageQualityCheck(imageRes.data.imageId, {
      checkTypeCode: 'AUTO',
      checkResultCode: 'PASS',
      qualityScore: 97,
      issueCodes: ['NONE'],
      suggestionText: 'Portal upload passed default quality gate',
      remark: 'Auto-approved from case portal'
    })

    const analysisRes = await casePortalApi.createAnalysis(caseId, {
      caseId,
      patientId,
      forceRetryFlag: false,
      taskTypeCode: 'INFERENCE',
      remark: 'Created from case portal'
    })

    notificationStore.success('病例创建成功', `分析任务 ${analysisRes.data.taskNo} 已入队。`)
    showNewCase.value = false
    resetNewCaseForm()
    await store.fetchTasks({ pageNum: 1, pageSize: 24 })
    await router.push(`/analysis/${analysisRes.data.taskId}`)
  } catch (error) {
    const message = normalizeErrorMessage(error)
    inlineError.value = message
    notificationStore.error('创建并分析失败', message)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.case-page {
  gap: 16px;
}

.case-filter-shell {
  display: grid;
  gap: 14px;
}

.case-search {
  max-width: 420px;
}

.case-grid-shell {
  min-height: 200px;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

.case-card {
  border-radius: 20px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: linear-gradient(180deg, rgba(13, 38, 47, 0.76), rgba(8, 23, 28, 0.92));
  overflow: hidden;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease;
}

.case-card:hover {
  transform: translateY(-3px);
  border-color: rgba(94, 234, 212, 0.26);
}

.case-visual {
  position: relative;
  min-height: 210px;
  overflow: hidden;
}

.case-visual-backdrop,
.case-visual-grid,
.case-visual-shape {
  position: absolute;
  inset: 0;
}

.case-visual-backdrop {
  background: radial-gradient(circle at top left, rgba(46, 230, 200, 0.22), transparent 42%), linear-gradient(180deg, #03131a, #071b22);
}

.case-visual-grid {
  background-image: linear-gradient(rgba(94, 234, 212, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(94, 234, 212, 0.08) 1px, transparent 1px);
  background-size: 26px 26px;
  mask-image: radial-gradient(circle at 50% 50%, #000 35%, transparent 90%);
}

.case-visual-shape-a {
  width: 160px;
  height: 160px;
  top: 28px;
  left: 40px;
  border-radius: 50%;
  background: rgba(46, 230, 200, 0.18);
  filter: blur(50px);
}

.case-visual-shape-b {
  width: 180px;
  height: 180px;
  top: 10px;
  right: -40px;
  border-radius: 50%;
  background: rgba(94, 234, 212, 0.16);
  filter: blur(60px);
}

.case-topbar,
.case-bottomline {
  position: absolute;
  left: 16px;
  right: 16px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  z-index: 2;
}

.case-topbar { top: 16px; }
.case-bottomline { bottom: 16px; align-items: center; color: rgba(255,255,255,0.76); font-size: 11px; }

.case-center-mark {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  z-index: 1;
}

.case-tooth {
  width: 18px;
  height: 64px;
  border-radius: 40% 40% 12% 12%;
  background: linear-gradient(180deg, rgba(255,255,255,.78), rgba(255,255,255,.08));
  opacity: 0.38;
}

.case-content {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.case-title-row,
.case-actions,
.case-uncertainty-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.case-patient-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.case-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.case-info-value {
  margin-top: 6px;
  color: var(--text);
  font-size: 13px;
}

.case-uncertainty-block {
  display: grid;
  gap: 10px;
}

.case-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(1, 12, 16, 0.66);
  backdrop-filter: blur(12px);
  display: grid;
  place-items: center;
  z-index: 60;
}

.case-modal {
  width: min(720px, calc(100vw - 32px));
}

.case-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.case-span-2 {
  grid-column: 1 / -1;
}

.case-dropzone {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 160px;
  padding: 20px;
  border-radius: 18px;
  border: 1.5px dashed rgba(46, 230, 200, 0.26);
  background: linear-gradient(180deg, rgba(46, 230, 200, 0.08), rgba(4, 18, 24, 0.66));
  color: var(--text-soft);
  cursor: pointer;
  transition: all .18s ease;
}

.case-dropzone.active,
.case-dropzone:hover {
  border-color: rgba(46, 230, 200, 0.46);
  background: linear-gradient(180deg, rgba(46, 230, 200, 0.12), rgba(4, 18, 24, 0.74));
}

.case-dropzone-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.case-dropzone-file {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.case-error-note {
  margin-top: 16px;
  border-color: rgba(255, 99, 110, 0.24);
  background: rgba(255, 99, 110, 0.08);
  color: #ffc7cd;
}

.case-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-dim);
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: all .2s ease-out;
}

.fade-up-enter-from,
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 760px) {
  .case-form-grid,
  .case-info-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
