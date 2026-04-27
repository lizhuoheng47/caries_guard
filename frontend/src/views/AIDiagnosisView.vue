<template>
  <div class="ai-diag">
    <div class="diag-grid">
      <!-- LEFT: image analysis -->
      <section class="card image-card">
        <div class="card-head">
          <div class="card-title">
            <span class="title-bar"></span>
            影像分析
          </div>
          <div v-if="state === 'result'" class="img-actions">
            <button class="ic-btn" @click="reset" title="重新上传">
              <svg viewBox="0 0 16 16" fill="none">
                <path d="M3 8a5 5 0 1 1 1.5 3.5M3 12V8h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="state === 'empty'" class="stage stage-empty"
             :class="{ 'is-drag': dragOver }"
             @dragover.prevent="dragOver = true"
             @dragleave.prevent="dragOver = false"
             @drop.prevent="onDrop"
             @click="triggerUpload">
          <div class="empty-bg">
            <div class="empty-grid"></div>

            <!-- Concentric ground rings -->
            <div class="holo-ring r1"></div>
            <div class="holo-ring r2"></div>
            <div class="holo-ring r3"></div>
            <div class="holo-ring r4"></div>
            <div class="holo-ring-fill"></div>
            <div class="holo-ring-arc"></div>

            <!-- Holographic molar (wireframe + glow) -->
            <div class="holo-tooth">
              <svg viewBox="0 0 220 280" fill="none">
                <defs>
                  <linearGradient id="hMolarStroke" x1="20" y1="0" x2="200" y2="280" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#bdf0ff"/>
                    <stop offset="50%" stop-color="#3aaaff"/>
                    <stop offset="100%" stop-color="#1a4cff"/>
                  </linearGradient>
                  <radialGradient id="hMolarFill" cx="0.5" cy="0.35" r="0.7">
                    <stop offset="0%" stop-color="#5fc6ff" stop-opacity="0.55"/>
                    <stop offset="55%" stop-color="#1a6fff" stop-opacity="0.18"/>
                    <stop offset="100%" stop-color="#0a1a4a" stop-opacity="0"/>
                  </radialGradient>
                  <filter id="hMolarGlow" x="-40%" y="-40%" width="180%" height="180%">
                    <feGaussianBlur stdDeviation="4.5" result="b"/>
                    <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
                  </filter>
                </defs>

                <g filter="url(#hMolarGlow)">
                  <!-- crown -->
                  <path d="M40 78 C 36 38, 76 8, 110 16 C 144 8, 184 38, 180 78 C 184 110, 168 130, 158 150 L 62 150 C 52 130, 36 110, 40 78 Z"
                        fill="url(#hMolarFill)" stroke="url(#hMolarStroke)" stroke-width="2"/>
                  <!-- cusps (top occlusal lines) -->
                  <path d="M58 60 Q 78 38 110 50 Q 142 38 162 60" stroke="#9ed8ff" stroke-width="1.6" fill="none" opacity="0.85"/>
                  <path d="M70 92 Q 90 76 110 86 Q 130 76 150 92" stroke="#9ed8ff" stroke-width="1.4" fill="none" opacity="0.7"/>
                  <!-- horizontal latitude rings (wireframe) -->
                  <ellipse cx="110" cy="50" rx="48" ry="8" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.55"/>
                  <ellipse cx="110" cy="78" rx="66" ry="10" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.55"/>
                  <ellipse cx="110" cy="110" rx="64" ry="9" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.45"/>
                  <ellipse cx="110" cy="140" rx="56" ry="8" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.4"/>
                  <!-- vertical wires -->
                  <path d="M110 18 V 150" stroke="#9ed8ff" stroke-width="1" opacity="0.45"/>
                  <path d="M75 22 Q 70 80 78 150" stroke="#9ed8ff" stroke-width="1" fill="none" opacity="0.35"/>
                  <path d="M145 22 Q 150 80 142 150" stroke="#9ed8ff" stroke-width="1" fill="none" opacity="0.35"/>

                  <!-- gum line -->
                  <path d="M62 150 L 158 150" stroke="#bdf0ff" stroke-width="1.5" opacity="0.5"/>

                  <!-- two roots -->
                  <path d="M78 150 C 70 178, 60 220, 76 250 C 86 268, 100 264, 104 240 L 104 150 Z"
                        fill="url(#hMolarFill)" stroke="url(#hMolarStroke)" stroke-width="1.8"/>
                  <path d="M142 150 C 150 178, 160 220, 144 250 C 134 268, 120 264, 116 240 L 116 150 Z"
                        fill="url(#hMolarFill)" stroke="url(#hMolarStroke)" stroke-width="1.8"/>
                  <!-- root inner detail -->
                  <path d="M89 160 Q 80 200 88 240" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.45"/>
                  <path d="M131 160 Q 140 200 132 240" stroke="#7fc7ff" stroke-width="1" fill="none" opacity="0.45"/>
                </g>

                <!-- highlight specular -->
                <path d="M58 38 Q 50 70 64 105" stroke="rgba(255,255,255,0.7)" stroke-width="2.5" stroke-linecap="round" fill="none"/>
              </svg>
              <div class="holo-shadow"></div>
            </div>

            <!-- Sweeping scan ring around base -->
            <div class="holo-sweep"></div>

            <!-- Floating particles -->
            <span v-for="p in particles" :key="p.id" class="emp-particle" :style="p.style"></span>
          </div>

          <div class="empty-text">
            <div class="empty-cta">
              <svg viewBox="0 0 24 24" fill="none">
                <path d="M12 16V4m0 0l-4 4m4-4l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              <span>点击或拖拽上传口腔影像</span>
            </div>
            <div class="empty-hint">支持&nbsp;&nbsp;JPG · PNG · DICOM&nbsp;&nbsp;·&nbsp;&nbsp;单个文件 ≤ 50MB</div>
            <button class="empty-btn" @click.stop="triggerUpload">
              <svg viewBox="0 0 18 18" fill="none">
                <path d="M9 4v10M4 9h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              上传影像
            </button>
          </div>

          <input ref="fileInput" type="file" accept="image/*" hidden @change="onPick"/>
        </div>

        <!-- Scanning state -->
        <div v-else-if="state === 'scanning'" class="stage stage-scan">
          <img :src="imageUrl" class="scan-img" alt="oral image"/>
          <div class="scan-overlay"></div>
          <div class="scan-bracket br-tl"></div>
          <div class="scan-bracket br-tr"></div>
          <div class="scan-bracket br-bl"></div>
          <div class="scan-bracket br-br"></div>

          <!-- Scan beam + trail -->
          <div class="scan-trail"></div>
          <div class="scan-beam"></div>
          <div class="scan-glow"></div>

          <!-- Grid noise -->
          <div class="scan-noise"></div>

          <div class="scan-status">
            <div class="status-dots"><span></span><span></span><span></span></div>
            <div class="status-text">AI 模型分析中…&nbsp;&nbsp;{{ progress }}%</div>
          </div>
        </div>

        <!-- Result state -->
        <div v-else class="stage stage-result">
          <img :src="imageUrl" class="scan-img" alt="oral image"/>
          <div class="scan-bracket br-tl"></div>
          <div class="scan-bracket br-tr"></div>
          <div class="scan-bracket br-bl"></div>
          <div class="scan-bracket br-br"></div>

          <!-- Annotated tooth markers (sample) -->
          <svg class="markers" viewBox="0 0 800 540" preserveAspectRatio="none">
            <g v-for="m in markers" :key="m.id">
              <rect :x="m.x" :y="m.y" :width="m.w" :height="m.h" rx="6"
                    fill="none" :stroke="m.color" stroke-width="2" stroke-dasharray="4 3"/>
              <circle :cx="m.x + m.w/2" :cy="m.y - 6" r="4" :fill="m.color"/>
            </g>
          </svg>
        </div>

        <!-- Result overview footer -->
        <div class="result-overview">
          <div class="overview-title">AI检测结果概览</div>
          <div class="overview-grid">
            <div class="ov-item ov-mint">
              <div class="ov-head">
                <span class="ov-icon" v-html="iconTooth"></span>
                <span class="ov-label">牙体检测</span>
              </div>
              <div class="ov-value">
                <span class="ov-num">{{ state === 'result' ? '28' : '—' }}</span>
                <span class="ov-unit">{{ state === 'result' ? '/ 28颗' : '' }}</span>
              </div>
              <div class="ov-cap">{{ state === 'result' ? '全部识别' : '等待分析' }}</div>
            </div>
            <div class="ov-item ov-rose">
              <div class="ov-head">
                <span class="ov-icon" v-html="iconWarn"></span>
                <span class="ov-label">异常区域</span>
              </div>
              <div class="ov-value">
                <span class="ov-num">{{ state === 'result' ? '7' : '—' }}</span>
                <span class="ov-unit">{{ state === 'result' ? '处' : '' }}</span>
              </div>
              <div class="ov-cap">{{ state === 'result' ? '需重点关注' : '等待分析' }}</div>
            </div>
            <div class="ov-item ov-amber">
              <div class="ov-head">
                <span class="ov-icon" v-html="iconLevel"></span>
                <span class="ov-label">严重程度</span>
              </div>
              <div class="ov-value">
                <span class="ov-num">{{ state === 'result' ? '中度' : '—' }}</span>
              </div>
              <div class="ov-cap">综合评估</div>
            </div>
            <div class="ov-item ov-mint">
              <div class="ov-head">
                <span class="ov-icon" v-html="iconShield"></span>
                <span class="ov-label">可信度评分</span>
              </div>
              <div class="ov-value">
                <span class="ov-num">{{ state === 'result' ? '92.7' : '—' }}</span>
                <span class="ov-unit" v-if="state === 'result'">%</span>
              </div>
              <div class="ov-cap">AI 置信度</div>
            </div>
          </div>
          <div class="disclaimer">* 结果由AI模型辅助生成，仅供临床参考，请结合医生临床判断。</div>
        </div>
      </section>

      <!-- RIGHT column -->
      <aside class="right-col">
        <!-- AI综合评估 -->
        <div class="card score-card">
          <div class="card-head">
            <div class="card-title">
              <span class="title-bar"></span>
              AI综合评估
              <svg class="info" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.4"/>
                <path d="M8 7v4M8 4.5h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
              </svg>
            </div>
          </div>

          <div class="score-body">
            <div class="score-ring">
              <svg viewBox="0 0 160 160">
                <defs>
                  <linearGradient id="ringGrad" x1="0" y1="0" x2="160" y2="160" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#2ee6c8"/>
                    <stop offset="40%" stop-color="#f7d63a"/>
                    <stop offset="75%" stop-color="#f77a3a"/>
                    <stop offset="100%" stop-color="#ff4d6e"/>
                  </linearGradient>
                </defs>
                <circle cx="80" cy="80" r="62" stroke="rgba(255,255,255,0.04)" stroke-width="10" fill="none"/>
                <circle cx="80" cy="80" r="62" stroke="url(#ringGrad)" stroke-width="10" fill="none"
                        stroke-linecap="round"
                        :stroke-dasharray="`${ringLen} 1000`"
                        transform="rotate(-90 80 80)"
                        :style="{ filter: 'drop-shadow(0 0 8px rgba(46,230,200,0.4))' }"/>
              </svg>
              <div class="score-center">
                <div class="score-num">{{ state === 'result' ? '85' : '—' }}</div>
                <div class="score-cap">{{ state === 'result' ? '/ 100' : '' }}</div>
                <div class="score-sub">综合评分</div>
              </div>
            </div>

            <div class="score-side">
              <span v-if="state === 'result'" class="risk-pill risk-high">高风险</span>
              <span v-else class="risk-pill risk-pending">待评估</span>
              <p class="score-desc">
                {{ state === 'result'
                   ? '存在多个需要处理的口腔问题，建议尽快制定治疗计划。'
                   : '上传影像后将生成AI评估。' }}
              </p>
            </div>
          </div>
        </div>

        <!-- 风险等级分布 -->
        <div class="card risk-card">
          <div class="card-head">
            <div class="card-title">
              <span class="title-bar"></span>
              风险等级分布
            </div>
          </div>

          <div class="risk-body">
            <div class="risk-donut">
              <svg viewBox="0 0 140 140">
                <circle cx="70" cy="70" r="50" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="14"/>
                <template v-if="state === 'result'">
                  <circle v-for="(s, i) in riskSegs" :key="i"
                          cx="70" cy="70" r="50" fill="none"
                          :stroke="s.color" stroke-width="14"
                          :stroke-dasharray="`${s.len} 1000`"
                          :stroke-dashoffset="s.offset"
                          transform="rotate(-90 70 70)"/>
                </template>
              </svg>
              <div class="risk-center">
                <div class="risk-num">{{ state === 'result' ? '7' : '—' }}</div>
                <div class="risk-cap">异常总数</div>
              </div>
            </div>

            <div class="risk-legend">
              <div class="r-row">
                <span class="r-dot" style="background:#ff4d6e"></span>
                <span>高风险</span>
                <span class="r-val">{{ state === 'result' ? '2 (28.6%)' : '—' }}</span>
              </div>
              <div class="r-row">
                <span class="r-dot" style="background:#f7a23a"></span>
                <span>中风险</span>
                <span class="r-val">{{ state === 'result' ? '3 (42.9%)' : '—' }}</span>
              </div>
              <div class="r-row">
                <span class="r-dot" style="background:#2ee6c8"></span>
                <span>低风险</span>
                <span class="r-val">{{ state === 'result' ? '2 (28.6%)' : '—' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- AI诊断结论 -->
        <div class="card concl-card">
          <div class="card-head">
            <div class="card-title">
              <span class="title-bar"></span>
              AI诊断结论
              <svg class="spark-i" viewBox="0 0 16 16" fill="none">
                <path d="M8 2l1.6 4.4L14 8l-4.4 1.6L8 14l-1.6-4.4L2 8l4.4-1.6L8 2Z" fill="#2ee6c8"/>
              </svg>
            </div>
          </div>

          <ul v-if="state === 'result'" class="concl-list">
            <li v-for="(c, i) in conclusions" :key="i">
              <span class="bullet" :class="`bullet-${c.tone}`">
                <svg viewBox="0 0 12 12" fill="none">
                  <path d="M3 6l2 2 4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              {{ c.text }}
            </li>
          </ul>
          <div v-else class="concl-empty">等待影像上传后生成 AI 诊断结论</div>

          <button class="report-btn" :disabled="state !== 'result'">
            <svg viewBox="0 0 18 18" fill="none">
              <path d="M5 2h6l3 3v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
              <path d="M7 9h4M7 12h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            生成诊疗建议报告
          </button>
        </div>

        <div class="footer-note">AI 赋能口腔影像诊断</div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'

type State = 'empty' | 'scanning' | 'result'
const state = ref<State>('empty')
const dragOver = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const imageUrl = ref<string>('')
const progress = ref(0)
let scanTimer: number | undefined

const particles = Array.from({ length: 22 }, (_, i) => {
  const left = (i * 47) % 100
  const top = (i * 31) % 100
  const size = i % 4 === 0 ? 3 : 2
  return {
    id: i,
    style: {
      left: `${left}%`,
      top: `${top}%`,
      width: `${size}px`,
      height: `${size}px`,
      animationDelay: `${(i % 6) * 0.4}s`,
      animationDuration: `${3 + (i % 5)}s`,
    },
  }
})

const triggerUpload = () => fileInput.value?.click()

const onPick = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) startScan(file)
}

const onDrop = (e: DragEvent) => {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) startScan(file)
}

const startScan = (file: File) => {
  imageUrl.value = URL.createObjectURL(file)
  state.value = 'scanning'
  progress.value = 0
  if (scanTimer) clearInterval(scanTimer)
  scanTimer = window.setInterval(() => {
    progress.value += 4
    if (progress.value >= 100) {
      progress.value = 100
      clearInterval(scanTimer)
      // small delay to let final beam finish
      setTimeout(() => { state.value = 'result' }, 400)
    }
  }, 120)
}

const reset = () => {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
  state.value = 'empty'
  progress.value = 0
}

onUnmounted(() => {
  if (scanTimer) clearInterval(scanTimer)
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
})

/* Score ring length (only when result) */
const ringLen = computed(() => {
  const C = 2 * Math.PI * 62
  return state.value === 'result' ? C * 0.85 : 0
})

/* Risk donut math */
const riskCirc = 2 * Math.PI * 50
const riskData = [
  { color: '#ff4d6e', pct: 28.6 },
  { color: '#f7a23a', pct: 42.9 },
  { color: '#2ee6c8', pct: 28.6 },
]
const riskSegs = computed(() => {
  let acc = 0
  return riskData.map(d => {
    const len = (d.pct / 100) * riskCirc
    const seg = { color: d.color, len, offset: -acc }
    acc += len
    return seg
  })
})

const markers = [
  { id: 1, x: 230, y: 270, w: 50, h: 90, color: '#9b6bff' },
  { id: 2, x: 285, y: 260, w: 50, h: 100, color: '#2ee6c8' },
  { id: 3, x: 470, y: 260, w: 50, h: 100, color: '#f7d63a' },
  { id: 4, x: 525, y: 270, w: 50, h: 90, color: '#ff7a3a' },
]

const conclusions = [
  { text: '右下智齿阻生，建议拔除', tone: 'rose' },
  { text: '左上第二前磨牙龋坏，建议充填治疗', tone: 'amber' },
  { text: '右下第一磨牙牙周炎，需进行牙周治疗', tone: 'rose' },
  { text: '定期复查，维护口腔健康', tone: 'mint' },
]

const iconTooth = `<svg viewBox="0 0 24 24" fill="none"><path d="M12 4c-3-3-9-1-9 5 0 4 3 7 3 11 0 1.6 1.4 1.5 2 .5l1.5-3 1.5 3.5 1.5-3.5 1.5 3c.6 1 2 1.1 2-.5 0-4 3-7 3-11 0-6-6-8-9-5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`
const iconWarn = `<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l9 16H3l9-16Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M12 10v4M12 17h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`
const iconLevel = `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.6"/><path d="M12 4v3M12 17v3M4 12h3M17 12h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`
const iconShield = `<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l8 3v6c0 5-3.4 9-8 11-4.6-2-8-6-8-11V6l8-3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`
</script>

<style scoped>
.ai-diag { display: flex; flex-direction: column; gap: 18px; }

.diag-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 1fr);
  gap: 14px;
  align-items: stretch;
}

/* Cards */
.card {
  position: relative;
  border-radius: 14px;
  border: 1px solid rgba(94, 234, 212, 0.1);
  background: linear-gradient(180deg, rgba(13, 38, 47, 0.7), rgba(8, 23, 28, 0.88));
  padding: 16px 18px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #e8fff8;
}

.title-bar {
  width: 3px;
  height: 14px;
  border-radius: 3px;
  background: #2ee6c8;
  box-shadow: 0 0 8px #2ee6c8;
}

.info, .spark-i { width: 14px; height: 14px; color: #5e8a82; margin-left: 4px; }

.image-card { display: flex; flex-direction: column; }

/* Stage (image area) */
.stage {
  position: relative;
  height: 420px;
  border-radius: 12px;
  border: 1px solid rgba(94, 234, 212, 0.15);
  overflow: hidden;
  background: #02131a;
}

/* Empty state */
.stage-empty {
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}
.stage-empty.is-drag {
  border-color: #2ee6c8;
  box-shadow: 0 0 24px rgba(46, 230, 200, 0.25), inset 0 0 32px rgba(46, 230, 200, 0.1);
}

.empty-bg {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background:
    radial-gradient(ellipse at 50% 60%, rgba(40, 110, 255, 0.32), transparent 60%),
    radial-gradient(ellipse at 50% 50%, rgba(46,230,200,0.06), transparent 70%),
    linear-gradient(180deg, #03101c, #02060f);
}

.empty-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(94, 180, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(94, 180, 255, 0.08) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at 50% 50%, #000 30%, transparent 80%);
}

/* Concentric ground rings (HUD plate) */
.holo-ring {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translate(-50%, 0) rotateX(72deg);
  border: 1.5px solid rgba(120, 200, 255, 0.55);
  border-radius: 50%;
  box-shadow: 0 0 14px rgba(80, 170, 255, 0.5), inset 0 0 16px rgba(80, 170, 255, 0.25);
}
.holo-ring.r1 { width: 230px; height: 230px; opacity: 0.95; animation: ringSpin 14s linear infinite; }
.holo-ring.r2 { width: 290px; height: 290px; opacity: 0.55; border-style: dashed; animation: ringSpin 22s linear infinite reverse; }
.holo-ring.r3 { width: 360px; height: 360px; opacity: 0.4; animation: ringSpin 30s linear infinite; }
.holo-ring.r4 { width: 430px; height: 430px; opacity: 0.22; border-style: dashed; }

.holo-ring-fill {
  position: absolute;
  bottom: 80px;
  left: 50%;
  width: 200px; height: 200px;
  transform: translate(-50%, 0) rotateX(72deg);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(60, 140, 255, 0.55), rgba(60, 140, 255, 0.1) 50%, transparent 75%);
  filter: blur(4px);
}

.holo-ring-arc {
  position: absolute;
  bottom: 80px;
  left: 50%;
  width: 290px; height: 290px;
  transform: translate(-50%, 0) rotateX(72deg);
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: #6ad3ff;
  box-shadow: 0 0 18px rgba(106, 211, 255, 0.7);
  animation: arcSpin 4.5s linear infinite;
}

.holo-tooth {
  position: relative;
  width: 220px;
  height: 280px;
  margin-bottom: 90px;
  z-index: 2;
  animation: holoFloat 4s ease-in-out infinite;
  filter: drop-shadow(0 0 14px rgba(60, 150, 255, 0.65));
}
.holo-tooth svg { width: 100%; height: 100%; }

.holo-shadow {
  position: absolute;
  bottom: -36px;
  left: 50%;
  transform: translateX(-50%);
  width: 130px;
  height: 16px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(60, 150, 255, 0.55), transparent 70%);
  filter: blur(4px);
}

.holo-sweep {
  position: absolute;
  bottom: 105px;
  left: 50%;
  width: 240px;
  height: 90px;
  transform: translateX(-50%);
  background: linear-gradient(to top, rgba(120, 220, 255, 0.5), transparent 80%);
  filter: blur(8px);
  mix-blend-mode: screen;
  animation: sweepUp 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes ringSpin {
  to { transform: translate(-50%, 0) rotateX(72deg) rotate(360deg); }
}
@keyframes arcSpin {
  to { transform: translate(-50%, 0) rotateX(72deg) rotate(360deg); }
}
@keyframes holoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}
@keyframes sweepUp {
  0%, 100% { opacity: 0.3; transform: translateX(-50%) scaleY(0.8); }
  50% { opacity: 0.85; transform: translateX(-50%) scaleY(1.1); }
}

.emp-particle {
  position: absolute;
  border-radius: 50%;
  background: #9ed8ff;
  box-shadow: 0 0 6px #5fc6ff;
  opacity: 0.5;
  animation: floatParticle 4s ease-in-out infinite;
}

.empty-text {
  position: absolute;
  bottom: 28px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 2;
  pointer-events: none;
}

.empty-cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #f5fffb;
  letter-spacing: 1px;
}
.empty-cta svg { width: 22px; height: 22px; color: #2ee6c8; }

.empty-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #5e8a82;
  letter-spacing: 1px;
}

.empty-btn {
  pointer-events: auto;
  margin-top: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  border: 1px solid rgba(46, 230, 200, 0.6);
  background: linear-gradient(135deg, rgba(46, 230, 200, 0.25), rgba(46, 230, 200, 0.08));
  color: #2ee6c8;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  box-shadow: 0 0 18px rgba(46, 230, 200, 0.25);
  transition: transform 0.18s ease, filter 0.18s ease;
}
.empty-btn svg { width: 14px; height: 14px; }
.empty-btn:hover { transform: translateY(-1px); filter: brightness(1.1); }

/* Scanning state */
.stage-scan { background: #000; }
.scan-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: brightness(0.78) contrast(1.05) hue-rotate(-10deg);
}

.scan-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 50%, transparent 50%, rgba(0,0,0,0.5)),
    linear-gradient(rgba(46,230,200,0.04), rgba(46,230,200,0.04));
  pointer-events: none;
}

.scan-noise {
  position: absolute;
  inset: 0;
  background-image:
    repeating-linear-gradient(0deg, rgba(46, 230, 200, 0.04) 0 1px, transparent 1px 3px);
  pointer-events: none;
  opacity: 0.5;
}

.scan-bracket {
  position: absolute;
  width: 22px;
  height: 22px;
  border-color: #2ee6c8;
  box-shadow: 0 0 8px rgba(46,230,200,0.6);
}
.br-tl { top: 12px; left: 12px; border-top: 2px solid; border-left: 2px solid; }
.br-tr { top: 12px; right: 12px; border-top: 2px solid; border-right: 2px solid; }
.br-bl { bottom: 12px; left: 12px; border-bottom: 2px solid; border-left: 2px solid; }
.br-br { bottom: 12px; right: 12px; border-bottom: 2px solid; border-right: 2px solid; }

/* The green scan beam + trail */
.scan-trail {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 130px;
  background: linear-gradient(
    to bottom,
    rgba(46, 230, 200, 0) 0%,
    rgba(46, 230, 200, 0.04) 30%,
    rgba(46, 230, 200, 0.18) 70%,
    rgba(46, 230, 200, 0.5) 95%,
    rgba(180, 255, 235, 0.95) 100%
  );
  mix-blend-mode: screen;
  animation: scanFall 2.4s cubic-bezier(.45,.05,.55,.95) infinite;
  pointer-events: none;
  filter: blur(0.5px);
}

.scan-beam {
  position: absolute;
  left: -2%;
  right: -2%;
  top: 130px;
  height: 3px;
  background: linear-gradient(90deg, transparent, #c4fff0 12%, #2ee6c8 50%, #c4fff0 88%, transparent);
  box-shadow:
    0 0 14px #2ee6c8,
    0 0 28px rgba(46, 230, 200, 0.65),
    0 0 60px rgba(46, 230, 200, 0.45);
  animation: scanBeam 2.4s cubic-bezier(.45,.05,.55,.95) infinite;
  pointer-events: none;
}

.scan-glow {
  position: absolute;
  left: -10%;
  right: -10%;
  top: 130px;
  height: 60px;
  background: radial-gradient(ellipse at center top, rgba(46, 230, 200, 0.45), transparent 70%);
  filter: blur(8px);
  mix-blend-mode: screen;
  animation: scanGlow 2.4s cubic-bezier(.45,.05,.55,.95) infinite;
  pointer-events: none;
}

@keyframes scanFall {
  0% { top: -130px; }
  100% { top: 100%; }
}
@keyframes scanBeam {
  0% { top: 0; }
  100% { top: calc(100% + 3px); }
}
@keyframes scanGlow {
  0% { top: -30px; }
  100% { top: calc(100% + 30px); }
}

.scan-status {
  position: absolute;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid rgba(46, 230, 200, 0.4);
  background: rgba(2, 18, 22, 0.7);
  backdrop-filter: blur(8px);
  color: #2ee6c8;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  z-index: 2;
}

.status-dots {
  display: inline-flex;
  gap: 4px;
}
.status-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #2ee6c8;
  box-shadow: 0 0 6px #2ee6c8;
  animation: pulseDot 1s ease-in-out infinite;
}
.status-dots span:nth-child(2) { animation-delay: 0.15s; }
.status-dots span:nth-child(3) { animation-delay: 0.3s; }

/* Result state */
.stage-result { background: #000; }
.markers {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.img-actions {
  display: flex;
  gap: 6px;
}
.ic-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid rgba(46, 230, 200, 0.3);
  background: rgba(46,230,200,0.08);
  color: #2ee6c8;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.ic-btn svg { width: 14px; height: 14px; }

/* Result overview */
.result-overview {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid rgba(94, 234, 212, 0.1);
  background: rgba(8, 23, 28, 0.6);
}

.overview-title {
  font-size: 13px;
  font-weight: 700;
  color: #e8fff8;
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.ov-item {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(94, 234, 212, 0.08);
  background: rgba(11, 36, 44, 0.45);
  --tone: #2ee6c8;
}
.ov-mint  { --tone: #2ee6c8; }
.ov-rose  { --tone: #ff636e; }
.ov-amber { --tone: #f7a23a; }

.ov-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #9bc6bd;
  margin-bottom: 6px;
}
.ov-icon { width: 14px; height: 14px; color: var(--tone); display: grid; place-items: center; }
.ov-icon svg,
.ov-icon :deep(svg) { width: 14px; height: 14px; }
.ov-label { font-weight: 600; color: var(--tone); }

.ov-value { display: flex; align-items: baseline; gap: 4px; }
.ov-num { font-size: 22px; font-weight: 800; color: #f5fffb; }
.ov-unit { font-size: 11px; color: #9bc6bd; }
.ov-cap { margin-top: 4px; font-size: 10px; color: #5e8a82; }

.disclaimer { margin-top: 10px; font-size: 10px; color: #5e8a82; letter-spacing: 0.5px; }

/* Right column */
.right-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Score card */
.score-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-ring {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}
.score-ring svg { width: 100%; height: 100%; }

.score-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}
.score-num {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  color: #f5fffb;
  text-shadow: 0 0 14px rgba(46, 230, 200, 0.4);
}
.score-cap { font-size: 11px; color: #9bc6bd; margin-top: 2px; }
.score-sub { font-size: 11px; color: #5e8a82; margin-top: 4px; }

.score-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.risk-pill {
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
}
.risk-high { color: #ff636e; background: rgba(255, 99, 110, 0.12); border: 1px solid rgba(255, 99, 110, 0.45); }
.risk-pending { color: #5e8a82; background: rgba(94, 138, 130, 0.1); border: 1px solid rgba(94, 138, 130, 0.4); }

.score-desc { margin: 0; font-size: 12px; color: #c5e2dc; line-height: 1.6; }

/* Risk donut */
.risk-body {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-donut { position: relative; width: 120px; height: 120px; flex-shrink: 0; }
.risk-donut svg { width: 100%; height: 100%; }
.risk-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.risk-num { font-size: 24px; font-weight: 800; color: #f5fffb; }
.risk-cap { font-size: 10px; color: #9bc6bd; }

.risk-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.r-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #c5e2dc;
}
.r-dot { width: 8px; height: 8px; border-radius: 50%; box-shadow: 0 0 6px currentColor; }
.r-val { margin-left: auto; color: #9bc6bd; font-size: 11px; }

/* Conclusion */
.concl-list {
  list-style: none;
  margin: 0 0 14px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.concl-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #c5e2dc;
  line-height: 1.5;
}

.bullet {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  margin-top: 1px;
}
.bullet svg { width: 10px; height: 10px; }
.bullet-rose { background: rgba(255,99,110,0.18); color: #ff636e; border: 1px solid rgba(255,99,110,0.5); }
.bullet-amber { background: rgba(247,162,58,0.18); color: #f7a23a; border: 1px solid rgba(247,162,58,0.5); }
.bullet-mint { background: rgba(46,230,200,0.18); color: #2ee6c8; border: 1px solid rgba(46,230,200,0.5); }

.concl-empty {
  padding: 24px 8px;
  text-align: center;
  font-size: 12px;
  color: #5e8a82;
  border: 1px dashed rgba(94, 234, 212, 0.18);
  border-radius: 10px;
  margin-bottom: 14px;
}

.report-btn {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  border: 1px solid rgba(46, 230, 200, 0.5);
  background: linear-gradient(135deg, rgba(46, 230, 200, 0.22), rgba(46, 230, 200, 0.08));
  color: #2ee6c8;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 0 18px rgba(46, 230, 200, 0.18);
  transition: transform 0.18s ease, filter 0.18s ease;
}
.report-btn svg { width: 16px; height: 16px; }
.report-btn:hover:not(:disabled) { transform: translateY(-1px); filter: brightness(1.08); }
.report-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.footer-note {
  text-align: center;
  font-size: 11px;
  color: #5e8a82;
  letter-spacing: 2px;
}

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
@keyframes shadowy {
  0%, 100% { transform: translateX(-50%) scale(1); opacity: 0.7; }
  50% { transform: translateX(-50%) scale(0.85); opacity: 0.5; }
}
@keyframes floatParticle {
  0%, 100% { transform: translateY(0); opacity: 0.4; }
  50% { transform: translateY(-12px); opacity: 0.9; }
}
@keyframes pulseDot {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}
</style>
