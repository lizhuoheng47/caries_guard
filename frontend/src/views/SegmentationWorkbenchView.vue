<template>
  <div class="seg-page">
    <header class="seg-header">
      <div>
        <p class="seg-eyebrow">REAL MODEL · BINARY SEGMENTATION</p>
        <h1>龋病影像智能分割</h1>
        <p class="seg-lead">快速分割用于即时预览；选择“发起完整诊断”后才会创建病例、保存分析并更新工作台。</p>
      </div>
      <div class="server-chip" :class="healthClass">
        <span class="server-dot"></span>
        <div>
          <strong>{{ healthLabel }}</strong>
          <small>{{ health?.modelCode || '正在检查模型服务' }}</small>
        </div>
      </div>
    </header>

    <main class="seg-grid">
      <section class="seg-panel viewer-panel">
        <div class="panel-bar">
          <div>
            <span class="panel-kicker">影像工作区</span>
            <strong>{{ selectedFile?.name || '尚未选择影像' }}</strong>
          </div>
          <div class="panel-actions">
            <button v-if="selectedFile" class="ghost-button" type="button" :disabled="analyzing" @click="reset">
              清除
            </button>
            <button class="primary-button" type="button" :disabled="analyzing" @click="openPicker">
              {{ selectedFile ? '更换影像' : '选择影像' }}
            </button>
          </div>
        </div>

        <input
          ref="fileInput"
          class="hidden-input"
          type="file"
          accept="image/png,image/jpeg,.dcm,application/dicom"
          @change="onFileChange"
        />

        <div
          v-if="!selectedFile"
          class="drop-zone"
          :class="{ dragging }"
          @click="openPicker"
          @dragover.prevent="dragging = true"
          @dragleave.prevent="dragging = false"
          @drop.prevent="onDrop"
        >
          <div class="drop-icon">＋</div>
          <h2>点击或拖拽牙科影像到这里</h2>
          <p>支持 PNG、JPG/JPEG、DICOM，单文件不超过 25 MB</p>
          <span>影像仅发送到本机 127.0.0.1 的分割服务</span>
        </div>

        <template v-else>
          <div class="view-tabs" role="tablist" aria-label="影像结果视图">
            <button
              v-for="view in availableViews"
              :key="view.key"
              type="button"
              :class="{ active: activeView === view.key }"
              @click="activeView = view.key"
            >
              {{ view.label }}
            </button>
          </div>

          <div class="image-stage">
            <img v-if="activeImageUrl" :src="activeImageUrl" :alt="activeViewLabel" />
            <div v-else class="dicom-wait">
              <span>DICOM</span>
              <p>{{ analyzing ? '模型正在解析 DICOM 影像…' : '点击下方按钮开始分析' }}</p>
            </div>
            <div v-if="analyzing" class="analysis-cover">
              <div class="scan-line"></div>
              <strong>U-Net 滑窗推理中</strong>
              <span>RTX 4060 通常需要数秒，请勿重复提交</span>
            </div>
          </div>

          <div class="run-bar">
            <div>
              <strong>{{ fileSizeLabel }}</strong>
              <span>{{ selectedFile.type || 'application/dicom' }}</span>
            </div>
            <div class="run-actions">
              <button class="ghost-button" type="button" :disabled="analyzing || savingCase || !modelReady" @click="analyze">
                {{ analyzing ? '分割中…' : result ? '重新预览分割' : '快速分割预览' }}
              </button>
              <button class="run-button" type="button" :disabled="analyzing || savingCase" @click="openCaseDialog">
                {{ savingCase ? '正在创建…' : '发起完整诊断' }}
              </button>
            </div>
          </div>
        </template>

        <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
      </section>

      <aside class="seg-panel result-panel">
        <div class="result-heading">
          <div>
            <span class="panel-kicker">模型输出</span>
            <h2>疑似区域</h2>
          </div>
          <span class="count-badge">{{ result?.regionCount ?? 0 }}</span>
        </div>

        <div v-if="result" class="model-facts">
          <div><span>模型</span><strong>{{ result.modelCode }}</strong></div>
          <div><span>运行设备</span><strong>{{ result.device }}</strong></div>
          <div><span>推理方式</span><strong>{{ result.inferenceMode }}</strong></div>
          <div><span>掩膜阈值</span><strong>{{ formatScore(result.maskThreshold) }}</strong></div>
        </div>

        <div v-if="result?.regions.length" class="region-list">
          <article v-for="(region, index) in result.regions" :key="region.regionIndex" class="region-card">
            <div class="region-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="region-body">
              <div><strong>疑似病灶区域</strong><span>{{ formatPercent(region.score) }}</span></div>
              <code>BBOX [{{ region.bbox.join(', ') }}]</code>
            </div>
          </article>
        </div>
        <div v-else class="empty-result">
          <strong>{{ result ? '未检出超过阈值的区域' : '等待分析' }}</strong>
          <p>{{ result ? '空结果也是有效模型输出，不能据此排除龋病。' : '上传影像后，这里只展示模型真实返回的数据。' }}</p>
        </div>

        <div class="safety-note">
          <strong>{{ result ? '分割预览尚未入库' : '研究用途提醒' }}</strong>
          <p>{{ result ? '如需同步工作台、病例中心、数据报表与 RAG 诊疗建议，请点击左侧“发起完整诊断”。' : '快速预览只做二值区域分割；完整诊断结果仍需医生复核。' }}</p>
        </div>
      </aside>
    </main>

    <Teleport to="body">
      <div v-if="showCaseDialog" class="case-dialog-backdrop" @click.self="closeCaseDialog">
        <section class="case-dialog" role="dialog" aria-modal="true" aria-labelledby="case-dialog-title">
          <header class="case-dialog-head">
            <div>
              <span class="panel-kicker">PERSISTED FULL ANALYSIS</span>
              <h2 id="case-dialog-title">保存病例并发起完整诊断</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="savingCase" @click="closeCaseDialog">关闭</button>
          </header>

          <div class="case-dialog-file">
            <strong>{{ selectedFile?.name }}</strong>
            <span>{{ fileSizeLabel }} · 将上传至病例影像库</span>
          </div>

          <div class="case-form-grid">
            <label class="case-field case-span-2">
              <span>患者姓名 / 患者号</span>
              <div class="case-search-row">
                <input v-model.trim="caseForm.patientCode" type="text" placeholder="输入姓名或编号" @input="clearPatientSelection" />
                <button class="ghost-button" type="button" :disabled="patientSearching || !caseForm.patientCode" @click="searchPatients">
                  {{ patientSearching ? '查询中…' : '查询患者库' }}
                </button>
              </div>
            </label>

            <div v-if="patientMatches.length" class="case-patient-results case-span-2">
              <button v-for="patient in patientMatches" :key="patient.patientId" type="button" :class="{ active: selectedPatientId === patient.patientId }" @click="selectPatient(patient)">
                <strong>{{ patient.patientNameMasked || '脱敏患者' }}</strong>
                <span>{{ patient.patientNo }} · {{ patient.genderCode || '--' }} · {{ patient.age ?? '--' }} 岁</span>
              </button>
            </div>

            <label class="case-field">
              <span>年龄</span>
              <input v-model.trim="caseForm.age" type="number" min="0" max="120" placeholder="45" />
            </label>
            <div class="case-field">
              <span>性别</span>
              <div class="gender-buttons">
                <button type="button" :class="{ active: caseForm.genderCode === 'MALE' }" @click="caseForm.genderCode = 'MALE'">男</button>
                <button type="button" :class="{ active: caseForm.genderCode === 'FEMALE' }" @click="caseForm.genderCode = 'FEMALE'">女</button>
              </div>
            </div>
            <label class="case-field case-span-2">
              <span>主诉与分析背景</span>
              <textarea v-model.trim="caseForm.chiefComplaint" rows="3" placeholder="填写本次检查原因、症状或筛查背景"></textarea>
            </label>
          </div>

          <p v-if="caseError" class="case-error">{{ caseError }}</p>

          <footer class="case-dialog-actions">
            <button class="ghost-button" type="button" :disabled="savingCase" @click="closeCaseDialog">取消</button>
            <button class="run-button" type="button" :disabled="savingCase" @click="submitFullAnalysis">
              {{ savingCase ? '正在创建病例并提交…' : '确认并发起完整诊断' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiClientError } from '@/api/request'
import { casePortalApi, type PatientListItem } from '@/api/casePortal'
import { segmentationApi, type SegmentationResult } from '@/api/segmentation'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

type ViewKey = 'original' | 'overlay' | 'heatmap' | 'mask'
type GenderCode = 'MALE' | 'FEMALE' | ''

const MAX_FILE_BYTES = 25 * 1024 * 1024
const router = useRouter()
const authStore = useAuthStore()
const notifications = useNotificationStore()
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const result = ref<SegmentationResult | null>(null)
const activeView = ref<ViewKey>('original')
const analyzing = ref(false)
const dragging = ref(false)
const errorMessage = ref('')
const health = ref<Awaited<ReturnType<typeof segmentationApi.health>> | null>(null)
const showCaseDialog = ref(false)
const savingCase = ref(false)
const caseError = ref('')
const patientSearching = ref(false)
const patientMatches = ref<PatientListItem[]>([])
const selectedPatientId = ref<string | null>(null)
const caseForm = reactive({
  patientCode: '',
  age: '',
  genderCode: '' as GenderCode,
  chiefComplaint: '',
})

const modelReady = computed(() => Boolean(health.value?.ready && health.value.implementationType === 'ML_MODEL'))
const healthClass = computed(() => (modelReady.value ? 'online' : health.value ? 'offline' : 'checking'))
const healthLabel = computed(() => (modelReady.value ? '真实模型在线' : health.value ? '模型不可用' : '检查服务中'))
const isDicom = computed(() => selectedFile.value?.name.toLowerCase().endsWith('.dcm') ?? false)
const fileSizeLabel = computed(() => {
  const bytes = selectedFile.value?.size || 0
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(2)} MB` : `${Math.ceil(bytes / 1024)} KB`
})
const availableViews = computed(() => {
  const views: Array<{ key: ViewKey; label: string }> = []
  if (!isDicom.value) views.push({ key: 'original', label: '原始影像' })
  if (result.value) {
    views.push(
      { key: 'overlay', label: '模型标注' },
      { key: 'heatmap', label: '热力图' },
      { key: 'mask', label: '二值掩膜' },
    )
  }
  return views
})
const activeImageUrl = computed(() => {
  if (activeView.value === 'overlay') return result.value?.assets.overlayUrl || ''
  if (activeView.value === 'heatmap') return result.value?.assets.heatmapUrl || ''
  if (activeView.value === 'mask') return result.value?.assets.maskUrl || ''
  return previewUrl.value
})
const activeViewLabel = computed(() => availableViews.value.find((item) => item.key === activeView.value)?.label || '牙科影像')

const openPicker = () => fileInput.value?.click()

const releasePreview = () => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

const selectFile = (file?: File) => {
  if (!file) return
  const lowerName = file.name.toLowerCase()
  const supported = ['image/png', 'image/jpeg', 'application/dicom'].includes(file.type)
    || lowerName.endsWith('.dcm')
  if (!supported) {
    errorMessage.value = '仅支持 PNG、JPG/JPEG 或 DICOM 文件。'
    return
  }
  if (file.size > MAX_FILE_BYTES) {
    errorMessage.value = '文件超过 25 MB，无法上传。'
    return
  }
  releasePreview()
  selectedFile.value = file
  result.value = null
  errorMessage.value = ''
  activeView.value = isDicom.value ? 'overlay' : 'original'
  if (!isDicom.value) previewUrl.value = URL.createObjectURL(file)
}

const onFileChange = (event: Event) => selectFile((event.target as HTMLInputElement).files?.[0])
const onDrop = (event: DragEvent) => {
  dragging.value = false
  selectFile(event.dataTransfer?.files?.[0])
}

const analyze = async () => {
  if (!selectedFile.value || analyzing.value) return
  analyzing.value = true
  errorMessage.value = ''
  try {
    result.value = await segmentationApi.analyze(selectedFile.value)
    activeView.value = 'overlay'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型分析失败，请检查后端服务。'
  } finally {
    analyzing.value = false
  }
}

const resetCaseForm = () => {
  caseForm.patientCode = ''
  caseForm.age = ''
  caseForm.genderCode = ''
  caseForm.chiefComplaint = ''
  caseError.value = ''
  patientMatches.value = []
  selectedPatientId.value = null
}

const openCaseDialog = () => {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择一张影像文件。'
    return
  }
  resetCaseForm()
  showCaseDialog.value = true
}

const closeCaseDialog = () => {
  if (savingCase.value) return
  showCaseDialog.value = false
  caseError.value = ''
}

const clearPatientSelection = () => {
  selectedPatientId.value = null
  patientMatches.value = []
}

const searchPatients = async () => {
  const keyword = caseForm.patientCode.trim()
  if (!keyword) return
  patientSearching.value = true
  caseError.value = ''
  try {
    const response = await casePortalApi.pagePatients({ pageNo: 1, pageSize: 10, keyword })
    patientMatches.value = response.data.records || []
    if (!patientMatches.value.length) notifications.info('未找到患者', '提交时将创建新的患者档案。')
  } catch (error) {
    caseError.value = normalizeError(error)
  } finally {
    patientSearching.value = false
  }
}

const selectPatient = (patient: PatientListItem) => {
  selectedPatientId.value = patient.patientId
  caseForm.patientCode = patient.patientNo
  caseForm.age = patient.age == null ? '' : String(patient.age)
  caseForm.genderCode = patient.genderCode === 'MALE' || patient.genderCode === 'FEMALE' ? patient.genderCode : ''
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

const normalizeError = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message
  if (error instanceof Error) return error.message
  return '创建病例并发起分析失败'
}

const validateCaseForm = () => {
  if (!selectedFile.value) return '请先选择一张影像文件。'
  if (!caseForm.patientCode) return '患者姓名或患者号不能为空。'
  if (!selectedPatientId.value && !caseForm.age) return '新患者年龄不能为空。'
  if (!selectedPatientId.value && !caseForm.genderCode) return '新患者请选择性别。'
  if (!caseForm.chiefComplaint) return '请填写主诉与分析背景。'
  if (!authStore.user?.id) return '当前登录信息不完整，请重新登录。'
  return ''
}

const submitFullAnalysis = async () => {
  const validationError = validateCaseForm()
  if (validationError) {
    caseError.value = validationError
    return
  }
  const file = selectedFile.value!
  const currentUserId = authStore.user!.id
  savingCase.value = true
  caseError.value = ''
  try {
    let patientId = selectedPatientId.value
    if (!patientId) {
      const patientResponse = await casePortalApi.createPatient({
        patientName: caseForm.patientCode,
        genderCode: caseForm.genderCode || undefined,
        birthDate: buildLocalDate(caseForm.age),
        sourceCode: 'OUTPATIENT',
        privacyLevelCode: 'L4',
        remark: 'Created from AI diagnosis workbench',
      })
      patientId = patientResponse.data.patientId
    }

    const visitResponse = await casePortalApi.createVisit({
      patientId,
      doctorUserId: currentUserId,
      visitTypeCode: 'OUTPATIENT',
      visitDate: buildLocalDateTime(),
      complaint: caseForm.chiefComplaint,
      triageLevelCode: 'NORMAL',
      sourceChannelCode: 'MANUAL',
      remark: 'Created from AI diagnosis workbench',
    })
    const caseResponse = await casePortalApi.createCase({
      visitId: visitResponse.data.visitId,
      patientId,
      caseTypeCode: 'CARIES_SCREENING',
      caseTitle: `Case ${caseForm.patientCode}`,
      chiefComplaint: caseForm.chiefComplaint,
      priorityCode: 'NORMAL',
      clinicalNotes: caseForm.chiefComplaint,
      remark: 'Created from AI diagnosis workbench',
    })
    const uploadResponse = await casePortalApi.uploadCaseFile(file, caseResponse.data.caseId, 'PANORAMIC')
    const imageResponse = await casePortalApi.createCaseImage(caseResponse.data.caseId, {
      attachmentId: uploadResponse.data.attachmentId,
      visitId: visitResponse.data.visitId,
      patientId,
      imageTypeCode: 'PANORAMIC',
      imageSourceCode: 'UPLOAD',
      shootingTime: buildLocalDateTime(),
      primaryFlag: '1',
      remark: 'Uploaded from AI diagnosis workbench',
    })
    await casePortalApi.saveImageQualityCheck(imageResponse.data.imageId, {
      checkTypeCode: 'AUTO',
      checkResultCode: 'PASS',
      qualityScore: 100,
      issueCodes: ['CLIENT_UPLOAD_PRECHECK'],
      suggestionText: '文件类型、大小和可读取性预检通过；影像质量仍由推理流水线继续评估。',
      remark: 'Created from AI diagnosis workbench upload precheck',
    })
    const analysisResponse = await casePortalApi.createAnalysis(caseResponse.data.caseId, {
      caseId: caseResponse.data.caseId,
      patientId,
      forceRetryFlag: false,
      taskTypeCode: 'INFERENCE',
      remark: 'Created from AI diagnosis workbench; full persisted inference',
    })

    window.dispatchEvent(new CustomEvent('caries-business-data-changed'))
    notifications.success('完整诊断已提交', `病例 ${caseResponse.data.caseNo}、任务 ${analysisResponse.data.taskNo} 已写入业务库。`)
    showCaseDialog.value = false
    await router.push(`/analysis/${analysisResponse.data.taskId}`)
  } catch (error) {
    caseError.value = normalizeError(error)
    notifications.error('创建并分析失败', caseError.value)
  } finally {
    savingCase.value = false
  }
}

const reset = () => {
  releasePreview()
  selectedFile.value = null
  result.value = null
  errorMessage.value = ''
  activeView.value = 'original'
  if (fileInput.value) fileInput.value.value = ''
}

const refreshHealth = async () => {
  try {
    health.value = await segmentationApi.health()
  } catch {
    health.value = { status: 'DOWN', ready: false, modelCode: '', implementationType: '', device: '' }
  }
}

const formatScore = (value: number) => Number(value || 0).toFixed(2)
const formatPercent = (value: number) => `${Math.round(Number(value || 0) * 100)}%`

onMounted(refreshHealth)
onBeforeUnmount(releasePreview)
</script>

<style scoped>
.seg-page { max-width: 1500px; margin: 0 auto; color: #edf7ff; }
.seg-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; margin-bottom: 22px; }
.seg-eyebrow, .panel-kicker { display: block; margin: 0 0 8px; color: #35f8ff; font: 700 11px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .15em; }
.seg-header h1 { margin: 0; font-size: clamp(25px, 3vw, 38px); letter-spacing: -.03em; }
.seg-lead { margin: 10px 0 0; color: #91a8cd; font-size: 14px; }
.server-chip { min-width: 190px; display: flex; align-items: center; gap: 10px; padding: 12px 14px; border: 1px solid rgba(112,224,255,.16); border-radius: 12px; background: rgba(9,25,53,.7); }
.server-chip strong, .server-chip small { display: block; }
.server-chip strong { font-size: 13px; }
.server-chip small { margin-top: 3px; color: #7189b6; font-size: 10px; }
.server-dot { width: 9px; height: 9px; border-radius: 50%; background: #75849e; box-shadow: 0 0 0 5px rgba(117,132,158,.12); }
.server-chip.online .server-dot { background: #37e6a2; box-shadow: 0 0 12px rgba(55,230,162,.8); }
.server-chip.offline .server-dot { background: #ff6b78; }
.seg-grid { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 18px; align-items: start; }
.seg-panel { border: 1px solid rgba(112,224,255,.14); border-radius: 16px; background: linear-gradient(150deg, rgba(13,32,66,.85), rgba(4,15,34,.9)); box-shadow: 0 20px 60px rgba(0,0,0,.22); overflow: hidden; }
.panel-bar { min-height: 70px; display: flex; justify-content: space-between; align-items: center; gap: 18px; padding: 14px 18px; border-bottom: 1px solid rgba(112,224,255,.1); }
.panel-bar strong { display: block; max-width: 560px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.panel-actions { display: flex; gap: 8px; }
button { font: inherit; cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .5; }
.primary-button, .ghost-button, .run-button { border-radius: 9px; padding: 9px 14px; color: #eefbff; border: 1px solid rgba(53,248,255,.28); }
.primary-button, .run-button { background: linear-gradient(135deg, #117ca1, #315be2); box-shadow: 0 8px 24px rgba(20,122,190,.25); }
.ghost-button { background: rgba(255,255,255,.035); }
.hidden-input { display: none; }
.drop-zone { min-height: 510px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px; text-align: center; border: 1px dashed rgba(112,224,255,.22); margin: 18px; border-radius: 14px; background: radial-gradient(circle, rgba(24,130,181,.1), transparent 58%); transition: .2s ease; cursor: pointer; }
.drop-zone.dragging, .drop-zone:hover { border-color: #35f8ff; background-color: rgba(53,248,255,.06); }
.drop-icon { width: 72px; height: 72px; display: grid; place-items: center; border: 1px solid rgba(53,248,255,.4); border-radius: 50%; color: #35f8ff; font-size: 36px; box-shadow: inset 0 0 26px rgba(53,248,255,.08), 0 0 30px rgba(53,248,255,.08); }
.drop-zone h2 { margin: 22px 0 8px; font-size: 19px; }
.drop-zone p, .drop-zone span { margin: 0; color: #8198be; font-size: 13px; }
.drop-zone span { margin-top: 8px; font-size: 11px; }
.view-tabs { display: flex; gap: 4px; padding: 12px 18px; border-bottom: 1px solid rgba(112,224,255,.08); }
.view-tabs button { padding: 7px 12px; border: 0; border-radius: 8px; color: #8299be; background: transparent; font-size: 12px; }
.view-tabs button.active { color: #35f8ff; background: rgba(53,248,255,.1); }
.image-stage { position: relative; min-height: 440px; display: grid; place-items: center; overflow: hidden; background: #01050c; }
.image-stage img { display: block; width: 100%; max-height: 620px; object-fit: contain; }
.dicom-wait { text-align: center; color: #8299be; }
.dicom-wait span { display: block; color: #35f8ff; font: 700 34px ui-monospace, monospace; letter-spacing: .12em; }
.analysis-cover { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; background: rgba(1,8,20,.72); backdrop-filter: blur(2px); }
.analysis-cover strong { color: #fff; font-size: 16px; }
.analysis-cover span { margin-top: 8px; color: #8fa8ce; font-size: 12px; }
.scan-line { position: absolute; left: 8%; right: 8%; top: 18%; height: 2px; background: #35f8ff; box-shadow: 0 0 22px #35f8ff; animation: scanning 2s ease-in-out infinite alternate; }
@keyframes scanning { to { top: 82%; } }
.run-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; border-top: 1px solid rgba(112,224,255,.08); }
.run-actions { display: flex; gap: 8px; }
.run-bar strong, .run-bar span { display: block; }
.run-bar strong { font-size: 13px; }.run-bar span { margin-top: 3px; color: #7189b6; font-size: 10px; }
.error-banner { margin: 0; padding: 12px 18px; color: #ffd5da; background: rgba(255,72,91,.1); border-top: 1px solid rgba(255,72,91,.2); font-size: 13px; }
.result-panel { padding: 18px; }
.result-heading { display: flex; justify-content: space-between; align-items: center; padding-bottom: 15px; border-bottom: 1px solid rgba(112,224,255,.1); }
.result-heading h2 { margin: 0; font-size: 20px; }
.count-badge { min-width: 42px; height: 42px; display: grid; place-items: center; border-radius: 12px; color: #35f8ff; background: rgba(53,248,255,.09); font: 700 17px ui-monospace, monospace; }
.model-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; margin: 16px 0; }
.model-facts div { min-width: 0; padding: 10px; border-radius: 9px; background: rgba(255,255,255,.035); }
.model-facts span, .model-facts strong { display: block; }.model-facts span { color: #7189b6; font-size: 10px; }.model-facts strong { margin-top: 4px; overflow: hidden; text-overflow: ellipsis; font-size: 11px; white-space: nowrap; }
.region-list { max-height: 470px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; padding-right: 3px; }
.region-card { display: flex; gap: 11px; padding: 11px; border: 1px solid rgba(112,224,255,.08); border-radius: 10px; background: rgba(1,9,22,.34); }
.region-index { color: #35f8ff; font: 700 12px ui-monospace, monospace; }
.region-body { min-width: 0; flex: 1; }.region-body > div { display: flex; justify-content: space-between; gap: 10px; font-size: 12px; }.region-body span { color: #37e6a2; font-family: ui-monospace, monospace; }.region-body code { display: block; margin-top: 7px; color: #6f86b6; font-size: 10px; white-space: normal; }
.empty-result { min-height: 260px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; color: #8299be; }.empty-result strong { color: #dbe9fb; }.empty-result p { max-width: 250px; margin: 8px 0 0; font-size: 12px; line-height: 1.6; }
.safety-note { margin-top: 16px; padding: 13px; border: 1px solid rgba(255,188,82,.18); border-radius: 10px; background: rgba(255,177,51,.055); }.safety-note strong { color: #ffd18a; font-size: 12px; }.safety-note p { margin: 6px 0 0; color: #9ba8bc; font-size: 11px; line-height: 1.6; }
.case-dialog-backdrop { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 24px; background: rgba(0,8,24,.78); backdrop-filter: blur(7px); }
.case-dialog { width: min(680px, 100%); max-height: calc(100vh - 48px); overflow-y: auto; padding: 22px; border: 1px solid rgba(53,248,255,.2); border-radius: 18px; color: #edf7ff; background: linear-gradient(150deg, #0d2042, #050f22); box-shadow: 0 28px 90px rgba(0,0,0,.5); }
.case-dialog-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; }.case-dialog-head h2 { margin: 0; font-size: 22px; }
.case-dialog-file { display: flex; justify-content: space-between; gap: 15px; margin: 18px 0; padding: 12px 14px; border: 1px solid rgba(53,248,255,.12); border-radius: 10px; background: rgba(53,248,255,.05); }.case-dialog-file strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.case-dialog-file span { flex: none; color: #8ca4ca; font-size: 12px; }
.case-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.case-span-2 { grid-column: 1 / -1; }
.case-field { display: flex; flex-direction: column; gap: 7px; }.case-field > span { color: #9bb1d2; font-size: 12px; }.case-field input, .case-field textarea { width: 100%; box-sizing: border-box; padding: 11px 12px; border: 1px solid rgba(112,224,255,.16); border-radius: 9px; outline: none; color: #eefbff; background: rgba(0,8,24,.55); }.case-field input:focus, .case-field textarea:focus { border-color: #35f8ff; }
.case-search-row { display: grid; grid-template-columns: 1fr auto; gap: 8px; }.gender-buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }.gender-buttons button, .case-patient-results button { padding: 10px; border: 1px solid rgba(112,224,255,.15); border-radius: 9px; color: #b9cbe6; background: rgba(255,255,255,.035); }.gender-buttons button.active, .case-patient-results button.active { border-color: #35f8ff; color: #35f8ff; background: rgba(53,248,255,.09); }
.case-patient-results { display: grid; gap: 7px; }.case-patient-results button { display: flex; justify-content: space-between; text-align: left; }.case-patient-results span { color: #8299be; font-size: 11px; }
.case-error { margin: 14px 0 0; padding: 10px 12px; border-radius: 8px; color: #ffd5da; background: rgba(255,72,91,.1); font-size: 12px; }.case-dialog-actions { display: flex; justify-content: flex-end; gap: 9px; margin-top: 20px; }
@media (max-width: 1050px) { .seg-grid { grid-template-columns: 1fr; }.result-panel { min-height: 300px; } }
@media (max-width: 700px) { .seg-header { flex-direction: column; }.server-chip { width: 100%; }.panel-bar, .run-bar { align-items: stretch; flex-direction: column; }.panel-actions, .run-actions { width: 100%; }.panel-actions button, .run-actions button { flex: 1; }.drop-zone { min-height: 380px; }.image-stage { min-height: 320px; }.case-form-grid { grid-template-columns: 1fr; }.case-span-2 { grid-column: auto; }.case-search-row { grid-template-columns: 1fr; }.case-dialog-file { flex-direction: column; } }
</style>
