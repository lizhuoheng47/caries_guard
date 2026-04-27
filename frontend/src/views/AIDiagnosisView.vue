<template>
  <div class="page diagnosis-page">
    <div class="page-hello diagnosis-hello">
      <div>
        <div class="micro">Oral Imaging Intelligence</div>
        <h1 class="page-hello-title">AI 诊断分析</h1>
        <p class="diagnosis-subtitle">
          上传口腔影像后，系统将模拟完成牙体识别、风险分层与诊疗建议汇总。
        </p>
      </div>

      <div class="diagnosis-hello-actions">
        <span class="diagnosis-meta">支持 JPG / PNG / DICOM</span>
        <span class="diagnosis-meta">单文件 ≤ 50MB</span>
        <button class="diag-btn diag-btn-ghost" type="button" :disabled="state === 'empty'" @click="reset">
          重新上传
        </button>
        <button class="diag-btn diag-btn-primary" type="button" @click="triggerUpload">
          上传影像
        </button>
      </div>
    </div>

    <div class="content-grid">
      <div class="left-column">
        <section class="panel image-panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">影像分析</div>
              <p class="panel-subtitle">支持拖拽上传，自动进入 AI 扫描流程。</p>
            </div>
            <div class="panel-head-actions">
              <span class="state-chip" :class="state">{{ stateLabel }}</span>
              <button
                v-if="state === 'result'"
                class="icon-btn"
                type="button"
                title="重新上传"
                @click="reset"
              >
                <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path
                    d="M3 8a5 5 0 1 1 1.5 3.5M3 12V8h4"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </div>
          </div>

          <div
            class="image-stage"
            @dragover.prevent="onDragOver"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop"
          >
            <div
              v-if="state === 'empty'"
              class="empty-state"
              :class="{ 'drag-over': isDragOver }"
              @click="triggerUpload"
            >
              <div class="tooth-wrap" aria-hidden="true">
                <div class="tooth-ring ring-1"></div>
                <div class="tooth-ring ring-2"></div>
                <div class="tooth-ring ring-3"></div>
                <svg class="tooth-svg" viewBox="0 0 220 280" fill="none">
                  <defs>
                    <radialGradient id="toothBody" cx="50%" cy="30%" r="60%">
                      <stop offset="0%" stop-color="#e4fff6" stop-opacity="0.98" />
                      <stop offset="42%" stop-color="#67ffd0" stop-opacity="0.72" />
                      <stop offset="72%" stop-color="#1bc5a2" stop-opacity="0.42" />
                      <stop offset="100%" stop-color="#0a4d3e" stop-opacity="0.18" />
                    </radialGradient>
                    <linearGradient id="toothRoot" x1="50%" y1="0%" x2="50%" y2="100%">
                      <stop offset="0%" stop-color="#2cfab5" stop-opacity="0.68" />
                      <stop offset="100%" stop-color="#0a4d3e" stop-opacity="0.08" />
                    </linearGradient>
                    <filter id="toothGlow">
                      <feGaussianBlur stdDeviation="7" result="blur" />
                      <feComposite in="SourceGraphic" in2="blur" operator="over" />
                    </filter>
                  </defs>

                  <path
                    d="M110 28c-22 0-39 16-41 37-3 22 4 43 16 64 7 13 13 24 17 35h16c4-11 10-22 17-35 12-21 19-42 16-64-2-21-19-37-41-37z"
                    fill="url(#toothBody)"
                    filter="url(#toothGlow)"
                  />
                  <path
                    d="M91 161c-4 18-9 40-13 61-2 10 1 18 5 22"
                    stroke="url(#toothRoot)"
                    stroke-width="8"
                    stroke-linecap="round"
                  />
                  <path
                    d="M129 161c4 18 9 40 13 61 2 10-1 18-5 22"
                    stroke="url(#toothRoot)"
                    stroke-width="8"
                    stroke-linecap="round"
                  />
                  <path
                    d="M110 161c0 16 0 40 0 62"
                    stroke="url(#toothRoot)"
                    stroke-width="6"
                    stroke-linecap="round"
                  />
                  <circle cx="64" cy="72" r="1.6" fill="#2cfab5" opacity="0.45" />
                  <circle cx="162" cy="86" r="1.2" fill="#2cfab5" opacity="0.36" />
                  <circle cx="78" cy="152" r="1" fill="#2cfab5" opacity="0.3" />
                  <circle cx="148" cy="146" r="1.4" fill="#2cfab5" opacity="0.34" />
                </svg>
                <div class="floor-grid"></div>
              </div>

              <div class="empty-copy">
                <div class="empty-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                    <path d="M12 16V4M12 4l-4 4M12 4l4 4" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" stroke-linecap="round" />
                  </svg>
                </div>
                <p class="empty-title">点击或拖拽上传口腔影像</p>
                <p class="empty-hint">支持 JPG、PNG、DICOM，单个文件不超过 50MB</p>
              </div>

              <button class="upload-btn" type="button" @click.stop="triggerUpload">
                <span>+</span>
                <span>上传影像</span>
              </button>
            </div>

            <div v-else-if="state === 'scanning'" class="scan-state">
              <div class="scan-box">
                <img :src="imagePreview" alt="待分析影像" class="stage-image" />
                <div class="scan-overlay"></div>
                <div class="scan-line"></div>
                <div class="scan-trail"></div>
                <div class="scan-corner tl"></div>
                <div class="scan-corner tr"></div>
                <div class="scan-corner bl"></div>
                <div class="scan-corner br"></div>
              </div>
              <div class="scan-progress">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${scanProgress}%` }"></div>
                </div>
                <span class="progress-text">AI 分析中... {{ scanProgress }}%</span>
              </div>
            </div>

            <div v-else class="result-state">
              <div class="result-box">
                <img :src="resultImagePreview" alt="分析结果影像" class="stage-image" />
                <div
                  v-for="annotation in activeAnnotations"
                  :key="annotation.id"
                  class="annotation-tooth-wrap"
                  :style="{
                    left: `${annotation.x}%`,
                    top: `${annotation.y}%`,
                    '--id': annotation.id
                  }"
                  @mouseenter="hoveredAnno = annotation.id"
                  @mouseleave="hoveredAnno = null"
                >
                  <svg class="annotation-tooth" viewBox="0 0 220 280" fill="none">
                    <defs>
                      <radialGradient :id="'toothBodyGrad-' + annotation.id" cx="50%" cy="30%" r="60%">
                        <stop offset="0%" :stop-color="annotation.color" stop-opacity="0.8" />
                        <stop offset="70%" :stop-color="annotation.color" stop-opacity="0.4" />
                        <stop offset="100%" :stop-color="annotation.color" stop-opacity="0.1" />
                      </radialGradient>
                      <linearGradient :id="'toothRootGrad-' + annotation.id" x1="50%" y1="0%" x2="50%" y2="100%">
                        <stop offset="0%" :stop-color="annotation.color" stop-opacity="0.6" />
                        <stop offset="100%" :stop-color="annotation.color" stop-opacity="0.1" />
                      </linearGradient>
                      <filter :id="'toothGlow-' + annotation.id">
                        <feGaussianBlur stdDeviation="8" result="blur" />
                        <feComposite in="SourceGraphic" in2="blur" operator="over" />
                      </filter>
                    </defs>

                    <g :filter="'url(#toothGlow-' + annotation.id + ')'">
                      <path
                        d="M110 28c-22 0-39 16-41 37-3 22 4 43 16 64 7 13 13 24 17 35h16c4-11 10-22 17-35 12-21 19-42 16-64-2-21-19-37-41-37z"
                        :fill="'url(#toothBodyGrad-' + annotation.id + ')'"
                      />
                      <path
                        d="M91 161c-4 18-9 40-13 61-2 10 1 18 5 22"
                        :stroke="'url(#toothRootGrad-' + annotation.id + ')'"
                        stroke-width="8"
                        stroke-linecap="round"
                      />
                      <path
                        d="M129 161c4 18 9 40 13 61 2 10-1 18-5 22"
                        :stroke="'url(#toothRootGrad-' + annotation.id + ')'"
                        stroke-width="8"
                        stroke-linecap="round"
                      />
                      <path
                        d="M110 161c0 16 0 40 0 62"
                        :stroke="'url(#toothRootGrad-' + annotation.id + ')'"
                        stroke-width="6"
                        stroke-linecap="round"
                      />
                    </g>
                  </svg>
                  <div v-if="hoveredAnno === annotation.id" class="annotation-tip">
                    {{ annotation.label }}
                  </div>
                </div>
              </div>
            </div>

            <input
              ref="fileInput"
              type="file"
              accept=".jpg,.jpeg,.png,.dcm,application/dicom,image/*"
              hidden
              @change="onFileSelected"
            />
          </div>
        </section>

        <section class="panel summary-panel">
          <div class="panel-head summary-head">
            <div class="panel-title">AI 检测结果概览</div>
            <span class="summary-tip">结果仅供临床辅助参考</span>
          </div>

          <div class="summary-grid">
            <article class="summary-card">
              <span class="summary-label">牙体检测</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.teethDetected }}</strong>
                  <small>/ 28 颗</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">{{ state === 'result' ? '全部识别' : '等待分析' }}</span>
            </article>

            <article class="summary-card warning">
              <span class="summary-label">异常区域</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.anomalies }}</strong>
                  <small>处</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">{{ state === 'result' ? '需重点关注' : '等待分析' }}</span>
            </article>

            <article class="summary-card">
              <span class="summary-label">严重程度</span>
              <div class="summary-value">
                <strong v-if="state === 'result'" class="severity">{{ activeResults.severity }}</strong>
                <template v-else>--</template>
              </div>
              <span class="summary-status">综合评估</span>
            </article>

            <article class="summary-card">
              <span class="summary-label">置信度评估</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.confidence }}</strong>
                  <small>%</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">AI 置信度</span>
            </article>
          </div>

          <p class="summary-note">
            * 结果由 AI 模型辅助生成，仅供临床参考，请结合医生诊断结论。
          </p>
        </section>
      </div>

      <div class="right-column">
        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">AI 综合评估</div>
            <button class="icon-btn" type="button" aria-label="说明">
              <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2">
                <circle cx="8" cy="8" r="7" />
                <path d="M8 5v0M8 7v4" />
              </svg>
            </button>
          </div>

          <div class="score-ring">
            <svg viewBox="0 0 120 120" aria-hidden="true">
              <defs>
                <linearGradient id="diagScoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#ff4757" />
                  <stop offset="35%" stop-color="#ff9f43" />
                  <stop offset="70%" stop-color="#ffd24a" />
                  <stop offset="100%" stop-color="#2cfab5" />
                </linearGradient>
              </defs>
              <circle cx="60" cy="60" r="52" stroke="#173036" stroke-width="8" fill="none" />
              <circle
                cx="60"
                cy="60"
                r="52"
                stroke="url(#diagScoreGrad)"
                stroke-width="8"
                fill="none"
                stroke-linecap="round"
                :stroke-dasharray="scoreCircumference"
                :stroke-dashoffset="scoreOffset"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="score-center">
              <strong>{{ state === 'result' ? activeResults.overallScore : '--' }}</strong>
              <span>{{ state === 'result' ? '/100' : '待评估' }}</span>
            </div>
          </div>

          <div class="risk-pill" :class="riskClass">{{ state === 'result' ? riskLabel : '待评估' }}</div>
          <p class="side-copy">
            {{
              state === 'result'
                ? '存在多个需要处理的口腔问题，建议尽快结合临床情况制定治疗方案。'
                : '上传影像后将生成 AI 风险评级与综合评估。'
            }}
          </p>
        </section>

        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">风险等级分布</div>
          </div>

          <div class="donut-wrap">
            <svg viewBox="0 0 120 120" class="donut-chart" aria-hidden="true">
              <circle cx="60" cy="60" r="45" stroke="#173036" stroke-width="14" fill="none" />
              <circle
                v-for="item in riskSegments"
                v-show="state === 'result' && item.length > 0"
                :key="item.key"
                cx="60"
                cy="60"
                r="45"
                :stroke="item.color"
                stroke-width="14"
                fill="none"
                :stroke-dasharray="`${item.length} ${donutCircumference}`"
                :stroke-dashoffset="item.offset"
                transform="rotate(-90 60 60)"
                stroke-linecap="round"
              />
            </svg>
            <div class="donut-center">
              <strong>{{ state === 'result' ? activeResults.anomalies : 0 }}</strong>
              <span>异常总数</span>
            </div>
          </div>

          <div class="risk-list">
            <div v-for="item in riskLegend" :key="item.key" class="risk-row">
              <div class="risk-label">
                <span class="risk-dot" :style="{ backgroundColor: item.color }"></span>
                <span>{{ item.label }}</span>
              </div>
              <strong>{{ state === 'result' ? `${item.count} (${item.percent})` : '--' }}</strong>
            </div>
          </div>
        </section>

        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">AI 诊断结论</div>
            <span class="sparkle">✦</span>
          </div>

          <div v-if="state === 'result'" class="diagnosis-list">
            <div
              v-for="(item, index) in activeResults.diagnoses"
              :key="`${item.level}-${index}`"
              class="diagnosis-item"
            >
              <span class="diag-icon" :style="{ color: diagColor(item.level) }">
                <svg viewBox="0 0 14 14" fill="none" aria-hidden="true">
                  <circle cx="7" cy="7" r="5" :stroke="diagColor(item.level)" stroke-width="1.2" />
                  <path
                    v-if="item.level === 'high'"
                    d="M5 5l4 4M9 5l-4 4"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                  />
                  <path
                    v-else-if="item.level === 'medium'"
                    d="M7 4.8v3.2M7 10v.01"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                  />
                  <path
                    v-else
                    d="M4.8 7.1l1.4 1.5L9.4 5.7"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
              <span>{{ item.text }}</span>
            </div>
          </div>
          <div v-else class="diagnosis-empty">等待影像上传后生成 AI 诊断结论</div>

          <button class="report-btn" type="button" :disabled="!hasResult">
            生成诊疗建议报告
          </button>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import {
  MOCK_ANNOTATIONS,
  MOCK_RESULTS,
  RISK_COLORS,
  type AnalysisResults,
  type Annotation,
  type DiagState,
  type RiskLevel,
} from './diagnosis'

const state = ref<DiagState>('empty')
const isDragOver = ref(false)
const imagePreview = ref('')
const scanProgress = ref(0)
const hoveredAnno = ref<number | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const SPECIAL_SCAN_FILE_NAME = '20_c1.jpg'
const SPECIAL_RESULT_IMAGE_URL = '/20show.png'
const specialUploadMatched = ref(false)

const specialResults: AnalysisResults = {
  ...MOCK_RESULTS,
  anomalies: 6,
  riskHigh: 2,
  riskMedium: 1,
  riskLow: 3,
}

const activeResults = computed<AnalysisResults>(() =>
  specialUploadMatched.value ? specialResults : MOCK_RESULTS,
)

const activeAnnotations = computed<Annotation[]>(() =>
  specialUploadMatched.value ? [] : MOCK_ANNOTATIONS,
)

const resultImagePreview = computed(() =>
  specialUploadMatched.value && state.value === 'result' ? SPECIAL_RESULT_IMAGE_URL : imagePreview.value,
)

let previewObjectUrl: string | null = null
let scanFrameId = 0
let finishTimerId = 0

const scoreCircumference = 2 * Math.PI * 52
const donutCircumference = 2 * Math.PI * 45
const hasResult = computed(() => state.value === 'result')

const stateLabel = computed(() => {
  switch (state.value) {
    case 'scanning':
      return '分析中'
    case 'result':
      return '已完成'
    default:
      return '等待上传'
  }
})

const scoreOffset = computed(() => {
  if (state.value !== 'result') return scoreCircumference
  return scoreCircumference - (activeResults.value.overallScore / 100) * scoreCircumference
})

const riskClass = computed(() => {
  if (state.value !== 'result') return 'pending'
  if (activeResults.value.overallScore >= 80) return 'high-risk'
  if (activeResults.value.overallScore >= 50) return 'medium-risk'
  return 'low-risk'
})

const riskLabel = computed(() => {
  if (activeResults.value.overallScore >= 80) return '高风险'
  if (activeResults.value.overallScore >= 50) return '中风险'
  return '低风险'
})

const totalRisk = computed(
  () => activeResults.value.riskHigh + activeResults.value.riskMedium + activeResults.value.riskLow,
)

const riskLegend = computed(() => {
  const items = [
    { key: 'high', label: '高风险', count: activeResults.value.riskHigh, color: '#ff4757' },
    { key: 'medium', label: '中风险', count: activeResults.value.riskMedium, color: '#ff9f43' },
    { key: 'low', label: '低风险', count: activeResults.value.riskLow, color: '#2cfab5' },
  ]

  return items.map((item) => ({
    ...item,
    percent: totalRisk.value === 0 ? '0%' : `${((item.count / totalRisk.value) * 100).toFixed(1)}%`,
  }))
})

const riskSegments = computed(() => {
  let consumed = 0

  return riskLegend.value.map((item) => {
    const length = totalRisk.value === 0 ? 0 : (item.count / totalRisk.value) * donutCircumference
    const segment = {
      ...item,
      length,
      offset: -consumed,
    }
    consumed += length
    return segment
  })
})

function diagColor(level: RiskLevel) {
  return RISK_COLORS[level]
}

function triggerUpload() {
  fileInput.value?.click()
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) startScan(file)
  target.value = ''
}

function onDragOver() {
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) startScan(file)
}

function startScan(file: File) {
  cleanupAnimation()
  revokePreviewUrl()
  hoveredAnno.value = null
  specialUploadMatched.value = isSpecialDemoUpload(file)
  imagePreview.value = buildPreviewUrl(file)
  scanProgress.value = 0
  state.value = 'scanning'
  animateScan()
}

function animateScan() {
  const duration = 3200
  const startTime = performance.now()

  const tick = (now: number) => {
    const elapsed = now - startTime
    scanProgress.value = Math.min(100, Math.round((elapsed / duration) * 100))

    if (scanProgress.value < 100) {
      scanFrameId = window.requestAnimationFrame(tick)
      return
    }

    finishTimerId = window.setTimeout(() => {
      state.value = 'result'
    }, 260)
  }

  scanFrameId = window.requestAnimationFrame(tick)
}

function reset() {
  cleanupAnimation()
  revokePreviewUrl()
  state.value = 'empty'
  imagePreview.value = ''
  scanProgress.value = 0
  hoveredAnno.value = null
  isDragOver.value = false
  specialUploadMatched.value = false

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function cleanupAnimation() {
  if (scanFrameId) {
    window.cancelAnimationFrame(scanFrameId)
    scanFrameId = 0
  }

  if (finishTimerId) {
    window.clearTimeout(finishTimerId)
    finishTimerId = 0
  }
}

function revokePreviewUrl() {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl)
    previewObjectUrl = null
  }
}

function buildPreviewUrl(file: File) {
  if (file.type.startsWith('image/')) {
    previewObjectUrl = URL.createObjectURL(file)
    return previewObjectUrl
  }

  return createDicomPlaceholder(file.name)
}

function isSpecialDemoUpload(file: File) {
  return file.name.trim().toLowerCase() === SPECIAL_SCAN_FILE_NAME
}

function createDicomPlaceholder(fileName: string) {
  const safeFileName = escapeForSvg(fileName.length > 24 ? `${fileName.slice(0, 21)}...` : fileName)
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#081519"/>
          <stop offset="60%" stop-color="#0d252a"/>
          <stop offset="100%" stop-color="#071014"/>
        </linearGradient>
      </defs>
      <rect width="1200" height="800" rx="36" fill="url(#bg)"/>
      <rect x="96" y="96" width="1008" height="608" rx="28" fill="none" stroke="#2cfab5" stroke-opacity="0.24" stroke-width="3"/>
      <circle cx="600" cy="325" r="118" fill="#2cfab5" fill-opacity="0.08" stroke="#2cfab5" stroke-opacity="0.5" stroke-width="3"/>
      <path d="M600 232c-32 0-57 23-61 55-4 31 7 60 23 92 10 20 18 39 23 55h30c5-16 13-35 23-55 16-32 27-61 23-92-4-32-29-55-61-55z" fill="#9ef7d7" fill-opacity="0.2" stroke="#2cfab5" stroke-width="3"/>
      <path d="M548 436c-8 33-19 93 3 125M652 436c8 33 19 93-3 125M600 436v136" stroke="#2cfab5" stroke-opacity="0.55" stroke-width="10" stroke-linecap="round"/>
      <text x="600" y="182" fill="#e8fff5" font-size="34" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">DICOM PREVIEW</text>
      <text x="600" y="618" fill="#94c9b7" font-size="28" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">${safeFileName}</text>
      <text x="600" y="662" fill="#5d8f80" font-size="20" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">当前页面为静态演示，占位图用于模拟 DICOM 导入效果</text>
    </svg>
  `

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function escapeForSvg(value: string) {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }

  return value.replace(/[&<>"']/g, (char) => map[char])
}

onUnmounted(() => {
  cleanupAnimation()
  revokePreviewUrl()
})
</script>

<style scoped>
.diagnosis-page {
  max-width: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 8px;
}

.diagnosis-hello {
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 0;
}

.diagnosis-subtitle {
  max-width: 720px;
  margin: 10px 0 0;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.7;
}

.diagnosis-hello-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.diagnosis-meta {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: rgba(11, 36, 44, 0.52);
  color: var(--text-soft);
  font-size: 12px;
}

.diag-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform .16s ease, filter .16s ease, background .16s ease;
}

.diag-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.diag-btn:disabled {
  opacity: .46;
  cursor: not-allowed;
}

.diag-btn-ghost {
  background: rgba(46, 230, 200, 0.08);
  border-color: rgba(46, 230, 200, 0.16);
  color: var(--brand-300);
}

.diag-btn-primary {
  background: linear-gradient(135deg, #6fffd6, #2ee6c8);
  color: #05211c;
  box-shadow: 0 10px 26px rgba(46, 230, 200, 0.16);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.left-column,
.right-column {
  min-width: 0;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  border-radius: 18px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: linear-gradient(180deg, rgba(11, 36, 44, 0.76), rgba(6, 20, 24, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.image-panel,
.summary-panel,
.side-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head.compact {
  align-items: center;
  margin-bottom: 14px;
}

.panel-title {
  position: relative;
  padding-left: 14px;
  color: var(--text);
  font-size: 17px;
  font-weight: 700;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #77ffdf, #2ee6c8);
}

.panel-subtitle,
.summary-tip {
  margin: 6px 0 0;
  color: var(--ink-3);
  font-size: 12px;
}

.panel-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.state-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(94, 234, 212, 0.14);
  background: rgba(94, 234, 212, 0.05);
  color: var(--text-soft);
  font-size: 12px;
}

.state-chip.scanning {
  color: var(--brand-300);
  background: rgba(46, 230, 200, 0.12);
}

.state-chip.result {
  color: #073026;
  border-color: transparent;
  background: linear-gradient(135deg, #82ffe0, #2ee6c8);
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(94, 234, 212, 0.14);
  border-radius: 10px;
  background: rgba(46, 230, 200, 0.06);
  color: var(--brand-300);
  cursor: pointer;
}

.icon-btn svg {
  width: 16px;
  height: 16px;
}

.image-stage {
  min-height: 420px;
}

.empty-state {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  padding: 32px 24px;
  border-radius: 16px;
  border: 1px dashed rgba(46, 230, 200, 0.16);
  background:
    radial-gradient(circle at 50% 24%, rgba(46, 230, 200, 0.08), transparent 36%),
    linear-gradient(180deg, rgba(5, 14, 17, 0.98), rgba(3, 9, 11, 0.98));
  overflow: hidden;
  cursor: pointer;
  transition: border-color .2s ease, box-shadow .2s ease;
}

.empty-state.drag-over {
  border-color: rgba(94, 234, 212, 0.4);
  box-shadow: 0 0 32px rgba(46, 230, 200, 0.12);
}

.tooth-wrap {
  position: relative;
  width: 220px;
  height: 260px;
  margin-bottom: 18px;
}

.tooth-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(94, 234, 212, 0.14);
  animation: pulseRing 3s ease-in-out infinite;
}

.ring-1 {
  width: 170px;
  height: 170px;
  top: 32px;
  left: 25px;
}

.ring-2 {
  width: 210px;
  height: 210px;
  top: 12px;
  left: 5px;
  animation-delay: .8s;
}

.ring-3 {
  width: 250px;
  height: 250px;
  top: -8px;
  left: -15px;
  animation-delay: 1.6s;
  opacity: .56;
}

.tooth-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 0 26px rgba(46, 230, 200, 0.24));
  animation: floatTooth 4s ease-in-out infinite;
}

.floor-grid {
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 290px;
  height: 56px;
  transform: translateX(-50%);
  border-radius: 50%;
  background:
    radial-gradient(ellipse at center, rgba(46, 230, 200, 0.1), transparent 70%),
    repeating-radial-gradient(circle at center, transparent 0 22px, rgba(46, 230, 200, 0.08) 22px 23px);
}

.empty-copy {
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin: 0 auto 10px;
  color: var(--brand-300);
}

.empty-icon svg {
  width: 24px;
  height: 24px;
}

.empty-title {
  margin: 0 0 6px;
  color: var(--text);
  font-size: 16px;
  font-weight: 600;
}

.empty-hint {
  margin: 0;
  color: var(--ink-3);
  font-size: 12px;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  min-height: 42px;
  padding: 0 22px;
  border-radius: 999px;
  border: 1px solid rgba(46, 230, 200, 0.36);
  background: rgba(46, 230, 200, 0.08);
  color: var(--brand-300);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.scan-state,
.result-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scan-box,
.result-box {
  position: relative;
  min-height: 380px;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: #010709;
}

.stage-image {
  display: block;
  width: 100%;
  min-height: 380px;
  max-height: 460px;
  object-fit: cover;
}

.scan-overlay {
  position: absolute;
  inset: 0;
  background: rgba(1, 8, 10, 0.24);
}

.scan-line,
.scan-trail {
  position: absolute;
  left: 0;
  right: 0;
  pointer-events: none;
}

.scan-line {
  height: 3px;
  background: #2ee6c8;
  box-shadow: 0 0 16px rgba(46, 230, 200, 0.85);
  animation: scanMove 2.4s ease-in-out infinite;
  z-index: 2;
}

.scan-trail {
  height: 120px;
  background: linear-gradient(to bottom, rgba(46, 230, 200, 0.22), transparent 80%);
  filter: blur(2px);
  animation: trailMove 2.4s ease-in-out infinite;
  z-index: 1;
}

.scan-corner {
  position: absolute;
  width: 22px;
  height: 22px;
  border-style: solid;
  border-color: #2ee6c8;
  border-width: 0;
  z-index: 3;
}

.scan-corner.tl {
  top: 10px;
  left: 10px;
  border-top-width: 2px;
  border-left-width: 2px;
}

.scan-corner.tr {
  top: 10px;
  right: 10px;
  border-top-width: 2px;
  border-right-width: 2px;
}

.scan-corner.bl {
  bottom: 10px;
  left: 10px;
  border-bottom-width: 2px;
  border-left-width: 2px;
}

.scan-corner.br {
  right: 10px;
  bottom: 10px;
  border-right-width: 2px;
  border-bottom-width: 2px;
}

.scan-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar {
  height: 6px;
  border-radius: 999px;
  background: #173036;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2ee6c8, #8fffe1);
  box-shadow: 0 0 16px rgba(46, 230, 200, 0.28);
}

.progress-text {
  color: var(--brand-300);
  font-size: 12px;
}

.annotation-tooth-wrap {
  position: absolute;
  width: 56px;
  height: 72px;
  transform: translate(-50%, -50%);
  z-index: 4;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.annotation-tooth-wrap:hover {
  transform: translate(-50%, -50%) scale(1.1);
  z-index: 5;
  filter: brightness(1.2);
}

.annotation-tooth {
  width: 100%;
  height: 100%;
  animation: floatTooth 4s ease-in-out infinite;
  animation-delay: calc(var(--id, 0) * 0.4s);
}

.annotation-tip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 4px);
  transform: translateX(-50%);
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid rgba(94, 234, 212, 0.14);
  background: rgba(3, 12, 15, 0.96);
  color: var(--text);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}

.summary-head {
  margin-bottom: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(94, 234, 212, 0.08);
  background: rgba(11, 36, 44, 0.42);
}

.summary-card.warning {
  border-color: rgba(255, 99, 110, 0.16);
}

.summary-label {
  color: var(--text-soft);
  font-size: 12px;
}

.summary-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  min-height: 34px;
  color: var(--text);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.summary-value strong {
  font-size: 28px;
}

.summary-value small {
  color: var(--ink-3);
  font-size: 12px;
  font-weight: 400;
}

.summary-value .severity {
  color: #ffb15f;
  font-size: 22px;
}

.summary-card.warning .summary-value strong {
  color: #ff7f7f;
}

.summary-status {
  color: var(--ink-3);
  font-size: 11px;
}

.summary-note {
  margin: 14px 0 0;
  color: var(--ink-3);
  font-size: 11px;
  line-height: 1.6;
}

.side-panel {
  display: flex;
  flex-direction: column;
}

.score-ring,
.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 14px;
}

.score-ring svg,
.donut-chart {
  width: 100%;
  height: 100%;
}

.score-center,
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-center strong,
.donut-center strong {
  color: var(--text);
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.score-center span,
.donut-center span {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.risk-pill {
  align-self: center;
  margin-bottom: 10px;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
}

.risk-pill.pending {
  color: var(--text-soft);
  background: rgba(94, 138, 130, 0.12);
  border-color: rgba(94, 138, 130, 0.2);
}

.risk-pill.high-risk {
  color: #ff636e;
  background: rgba(255, 99, 110, 0.14);
  border-color: rgba(255, 99, 110, 0.2);
}

.risk-pill.medium-risk {
  color: #f7a23a;
  background: rgba(247, 162, 58, 0.14);
  border-color: rgba(247, 162, 58, 0.2);
}

.risk-pill.low-risk {
  color: var(--brand-300);
  background: rgba(46, 230, 200, 0.14);
  border-color: rgba(46, 230, 200, 0.2);
}

.side-copy {
  margin: 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.7;
  text-align: center;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-row,
.risk-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.risk-label {
  justify-content: flex-start;
  color: var(--text-soft);
  flex: 1;
}

.risk-row strong {
  color: var(--ink-2);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sparkle {
  color: var(--brand-300);
  font-size: 14px;
}

.diagnosis-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.diagnosis-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 12px;
  background: rgba(11, 36, 44, 0.42);
  border: 1px solid rgba(94, 234, 212, 0.06);
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.diag-icon {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

.diag-icon svg {
  width: 18px;
  height: 18px;
}

.diagnosis-empty {
  margin-bottom: 16px;
  padding: 28px 14px;
  border-radius: 14px;
  border: 1px dashed rgba(94, 234, 212, 0.12);
  background: rgba(11, 36, 44, 0.22);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

.report-btn {
  width: 100%;
  min-height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(46, 230, 200, 0.16);
  background: rgba(46, 230, 200, 0.06);
  color: var(--ink-3);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.report-btn:not(:disabled) {
  color: var(--brand-300);
  border-color: rgba(46, 230, 200, 0.36);
  background: linear-gradient(135deg, rgba(46, 230, 200, 0.14), rgba(46, 230, 200, 0.06));
}

.report-btn:disabled {
  cursor: not-allowed;
}

@keyframes floatTooth {
  0%, 100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@keyframes pulseRing {
  0%, 100% {
    transform: scale(1);
    opacity: .32;
  }

  50% {
    transform: scale(1.05);
    opacity: .62;
  }
}

@keyframes scanMove {
  0% {
    top: -3px;
  }

  50% {
    top: 100%;
  }

  50.01% {
    top: -3px;
  }

  100% {
    top: 100%;
  }
}

@keyframes trailMove {
  0% {
    top: -120px;
  }

  50% {
    top: calc(100% - 120px);
  }

  50.01% {
    top: -120px;
  }

  100% {
    top: calc(100% - 120px);
  }
}



@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .right-column {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .diagnosis-hello {
    flex-direction: column;
  }

  .diagnosis-hello-actions {
    justify-content: flex-start;
  }

  .summary-grid,
  .right-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .summary-grid,
  .right-column {
    grid-template-columns: minmax(0, 1fr);
  }

  .stage-image,
  .scan-box,
  .result-box {
    min-height: 280px;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
