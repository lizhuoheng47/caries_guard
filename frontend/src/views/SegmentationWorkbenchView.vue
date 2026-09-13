<template>
  <div class="seg-page">
    <header class="seg-header">
      <div>
        <p class="seg-eyebrow">REAL MODEL · BINARY SEGMENTATION</p>
        <h1>龋病影像智能分割</h1>
        <p class="seg-lead">上传全景牙片，由本地 U-Net 自动定位疑似龋病区域。页面不生成预置结论。</p>
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
            <button class="run-button" type="button" :disabled="analyzing || !modelReady" @click="analyze">
              {{ analyzing ? '正在分析…' : result ? '重新分析' : '开始真实模型分析' }}
            </button>
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
          <strong>研究用途提醒</strong>
          <p>当前模型只做二值区域分割，不推断牙位、病灶深度、严重程度或治疗方案，所有结果均需人工复核。</p>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { segmentationApi, type SegmentationResult } from '@/api/segmentation'

type ViewKey = 'original' | 'overlay' | 'heatmap' | 'mask'

const MAX_FILE_BYTES = 25 * 1024 * 1024
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const result = ref<SegmentationResult | null>(null)
const activeView = ref<ViewKey>('original')
const analyzing = ref(false)
const dragging = ref(false)
const errorMessage = ref('')
const health = ref<Awaited<ReturnType<typeof segmentationApi.health>> | null>(null)

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
@media (max-width: 1050px) { .seg-grid { grid-template-columns: 1fr; }.result-panel { min-height: 300px; } }
@media (max-width: 700px) { .seg-header { flex-direction: column; }.server-chip { width: 100%; }.panel-bar, .run-bar { align-items: stretch; flex-direction: column; }.panel-actions { width: 100%; }.panel-actions button, .run-button { flex: 1; }.drop-zone { min-height: 380px; }.image-stage { min-height: 320px; } }
</style>
