<template>
  <div class="page">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">Case Portal</div>
        <h1 class="page-hello-title">病例入口与影像发起</h1>
      </div>
      <button class="btn btn-primary" @click="openNewCaseDrawer">
        <AppIcon name="plus" :size="14" />
        新建病例
      </button>
    </div>

    <div class="card" style="padding: 16px; margin-bottom: 16px">
      <div style="display: flex; gap: 12px; justify-content: space-between; align-items: center; flex-wrap: wrap">
        <div class="lib-search" style="width: 320px">
          <AppIcon name="search" :size="15" />
          <input v-model.trim="searchKeyword" type="text" placeholder="搜索病例号、患者号、任务号" />
        </div>
        <div class="lib-filter-group">
          <button
            v-for="item in statusTabs"
            :key="item.code"
            class="lib-filter"
            :class="{ on: activeStatus === item.code }"
            @click="activeStatus = item.code"
          >
            <span>{{ item.label }}</span>
            <span class="mono">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="lib-grid">
      <article
        v-for="item in filteredItems"
        :key="item.id"
        class="lib-card"
        @click="router.push(`/analysis/${item.id}`)"
      >
        <div class="lib-card-img" style="background: linear-gradient(180deg, #0c1317, #18252c)">
          <div class="lib-card-tags">
            <span class="chip chip-neutral">{{ item.caseNo || 'CASE' }}</span>
            <span class="chip" :style="statusChipStyle(item.status)">{{ statusLabel(item.status) }}</span>
          </div>
          <div style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center">
            <div style="display: flex; gap: 4px; opacity: .38">
              <div v-for="t in 6" :key="t" style="width: 18px; height: 62px; border-radius: 40% 40% 10% 10%; background: linear-gradient(180deg, rgba(255,255,255,.7), rgba(255,255,255,.12))"></div>
            </div>
          </div>
          <div style="position: absolute; left: 14px; right: 14px; bottom: 12px; display: flex; justify-content: space-between; align-items: center">
            <span class="mono" style="font-size: 10px; color: rgba(255,255,255,.75)">{{ formatDate(item.createdAt) }}</span>
            <span class="chip" :style="gradeChipStyle(item.grade)">{{ item.grade || '--' }}</span>
          </div>
        </div>

        <div class="lib-card-body">
          <div class="lib-card-row">
            <div class="lib-card-name">{{ item.patientName || item.patientId || '未命名患者' }}</div>
            <div class="mono" style="font-size: 11px; color: var(--ink-3)">{{ item.no }}</div>
          </div>
          <div class="lib-card-tooth">{{ item.patientId || 'PENDING' }}</div>

          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 14px">
            <div style="display: flex; flex-direction: column; gap: 4px">
              <span class="micro">Uncertainty</span>
              <span class="mono" style="font-size: 12px; color: var(--ink-2)">{{ (item.uncertainty || 0).toFixed(2) }}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px">
              <div style="width: 90px; height: 8px; background: var(--surface-sunk); border-radius: 999px; overflow: hidden">
                <div
                  :style="{
                    width: `${Math.min(100, Math.max(0, (item.uncertainty || 0) * 100))}%`,
                    height: '100%',
                    background: (item.uncertainty || 0) >= 0.35 ? 'var(--warn-500)' : 'var(--brand-500)'
                  }"
                ></div>
              </div>
              <AppIcon name="chevron_right" :size="14" />
            </div>
          </div>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <Transition name="fade-up">
        <div v-if="showNewCase" class="case-modal-mask" @click.self="closeNewCaseDrawer">
          <div class="case-modal card">
            <div class="card-head">
              <h3>新建病例</h3>
              <button class="btn btn-subtle btn-sm" :disabled="isSubmitting" @click="closeNewCaseDrawer">关闭</button>
            </div>

            <div style="padding: 20px; display: grid; gap: 16px">
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
                <label style="display: grid; gap: 6px">
                  <span class="micro">患者编号</span>
                  <input v-model.trim="newCaseForm.patientCode" class="case-input" type="text" placeholder="P-10001" />
                </label>
                <label style="display: grid; gap: 6px">
                  <span class="micro">年龄</span>
                  <input v-model.trim="newCaseForm.age" class="case-input" type="number" min="0" max="120" placeholder="45" />
                </label>
              </div>

              <div style="display: grid; gap: 6px">
                <span class="micro">性别</span>
                <div class="lib-filter-group">
                  <button class="lib-filter" :class="{ on: newCaseForm.genderCode === 'MALE' }" @click="newCaseForm.genderCode = 'MALE'">男</button>
                  <button class="lib-filter" :class="{ on: newCaseForm.genderCode === 'FEMALE' }" @click="newCaseForm.genderCode = 'FEMALE'">女</button>
                </div>
              </div>

              <label style="display: grid; gap: 6px">
                <span class="micro">主诉</span>
                <textarea v-model.trim="newCaseForm.chiefComplaint" class="case-input" style="min-height: 96px; resize: vertical; padding-top: 12px" placeholder="填写患者本次就诊的主诉与影像分析背景"></textarea>
              </label>

              <div style="display: grid; gap: 6px">
                <span class="micro">影像上传</span>
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".png,.jpg,.jpeg,.dcm,.dicom,image/png,image/jpeg,application/dicom"
                  style="display: none"
                  @change="handleFileInputChange"
                />
                <div
                  class="case-drop"
                  :class="{ on: isDragActive }"
                  @click="openFilePicker"
                  @dragenter.prevent="isDragActive = true"
                  @dragover.prevent="isDragActive = true"
                  @dragleave.prevent="isDragActive = false"
                  @drop.prevent="handleFileDrop"
                >
                  <template v-if="selectedFile">
                    <div style="display: flex; justify-content: space-between; gap: 12px; width: 100%; align-items: start">
                      <div style="display: grid; gap: 4px; min-width: 0">
                        <strong style="font-size: 13px">{{ selectedFile.name }}</strong>
                        <span class="mono" style="font-size: 11px; color: var(--ink-3)">{{ formatFileSize(selectedFile.size) }} · {{ fileTypeLabel }}</span>
                      </div>
                      <button class="btn btn-subtle btn-sm" @click.stop="clearSelectedFile">移除</button>
                    </div>
                  </template>
                  <template v-else>
                    <AppIcon name="upload" :size="22" />
                    <div class="home-drop-t">拖拽影像到这里，或点击选择文件</div>
                    <div class="home-drop-s">支持 PNG / JPG / DICOM</div>
                  </template>
                </div>
              </div>

              <div v-if="inlineError" class="card" style="padding: 12px 14px; background: var(--danger-100); border-color: #f5c7c3; color: var(--danger-700)">
                {{ inlineError }}
              </div>

              <div style="display: flex; justify-content: end; gap: 10px">
                <button class="btn btn-ghost" :disabled="isSubmitting" @click="closeNewCaseDrawer">取消</button>
                <button class="btn btn-primary" :disabled="isSubmitting" @click="submitNewCase">
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
const activeStatus = ref<'ALL' | 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED'>('ALL')

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

const statusChipStyle = (status: string) => {
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
      return 'background: rgba(255,255,255,.85); color: var(--ink-2);'
  }
}

const statusTabs = computed(() => {
  const count = (status: AnalysisTaskItem['status']) => store.tasks.items.filter((item) => item.status === status).length
  return [
    { code: 'ALL' as const, label: '全部', count: store.tasks.items.length },
    { code: 'DONE' as const, label: '完成', count: count('DONE') },
    { code: 'RUNNING' as const, label: '运行中', count: count('RUNNING') },
    { code: 'REVIEW' as const, label: '复核', count: count('REVIEW') },
    { code: 'FAILED' as const, label: '失败', count: count('FAILED') }
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
  void store.fetchTasks({ pageNum: 1, pageSize: 12 })
})

const formatDate = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleDateString()
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
  if (!currentUserId) {
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
    await store.fetchTasks({ pageNum: 1, pageSize: 12 })
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
.case-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(11, 26, 32, 0.48);
  backdrop-filter: blur(10px);
  display: grid;
  place-items: center;
  z-index: 50;
}

.case-modal {
  width: min(680px, calc(100vw - 32px));
  overflow: hidden;
}

.case-input {
  width: 100%;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface-2);
  color: var(--ink-1);
  outline: none;
  transition: border-color .15s ease;
}

.case-input:focus {
  border-color: var(--brand-500);
  box-shadow: var(--glow-brand);
}

.case-drop {
  display: grid;
  gap: 8px;
  place-items: center;
  padding: 28px 20px;
  border: 1.5px dashed var(--brand-300);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--brand-50), #fff);
  transition: all .18s ease;
  cursor: pointer;
}

.case-drop.on,
.case-drop:hover {
  border-color: var(--brand-500);
  background: var(--brand-100);
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
</style>
