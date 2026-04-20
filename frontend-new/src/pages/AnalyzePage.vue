<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import ToothImage from '@/components/ToothImage.vue'

const { locale } = useI18n()

interface Lesion {
  id: string; tooth: string; surface: string
  x: number; y: number; w: number; h: number
  sev: number; conf: number; size: string; rec: string
}

const LESIONS: Lesion[] = [
  { id: 'L1', tooth: '16', surface: 'O',  x: 225, y: 185, w: 48, h: 44, sev: 3, conf: 0.94, size: '3.1 mm', rec: '树脂充填 · Composite restoration' },
  { id: 'L2', tooth: '26', surface: 'DO', x: 425, y: 175, w: 42, h: 50, sev: 2, conf: 0.87, size: '1.8 mm', rec: '窝沟封闭 · Sealant' },
  { id: 'L3', tooth: '14', surface: 'M',  x: 315, y: 200, w: 28, h: 36, sev: 1, conf: 0.72, size: '0.9 mm', rec: '观察 + 氟化 · Observe + fluoride' },
  { id: 'L4', tooth: '11', surface: 'L',  x: 540, y: 190, w: 38, h: 48, sev: 4, conf: 0.97, size: '5.4 mm', rec: '根管治疗 · Root canal therapy' }
]

type Phase = 'idle' | 'uploading' | 'analyzing' | 'result'
const phase = ref<Phase>('result')
const progress = ref(0)
const activeLesion = ref('L1')
const decisions = ref<Record<string, string>>({ L1: 'pending', L2: 'pending', L3: 'pending', L4: 'pending' })
const tool = ref('move')
const zoom = ref(1)

const active = computed(() => LESIONS.find(l => l.id === activeLesion.value)!)
const sevLabel = (s: number) => ['', '浅龋 Initial', '中龋 Moderate', '深龋 Deep', '牙髓 Pulpal'][s]

const runAnalyze = () => {
  phase.value = 'uploading'; progress.value = 0
  let p = 0
  const up = setInterval(() => {
    p += 8 + Math.random() * 6
    if (p >= 100) { p = 100; clearInterval(up); phase.value = 'analyzing'; setTimeout(analyze, 200) }
    progress.value = p
  }, 90)
  const analyze = () => {
    let q = 0
    const an = setInterval(() => {
      q += 3 + Math.random() * 3
      if (q >= 100) { q = 100; clearInterval(an); setTimeout(() => { phase.value = 'result' }, 400) }
      progress.value = q
    }, 70)
  }
}

const toolClick = (id: string) => {
  tool.value = id
  if (id === 'zoom_in') zoom.value = Math.min(zoom.value + 0.2, 2)
  if (id === 'zoom_out') zoom.value = Math.max(zoom.value - 0.2, 0.6)
}

const tools = [
  { id: 'move', zh: '移动', en: 'Move' },
  { id: 'rect', zh: '画框', en: 'Box' },
  { id: 'pen', zh: '标注', en: 'Pen' },
  { id: 'zoom_in', zh: '放大', en: 'Zoom+' },
  { id: 'zoom_out', zh: '缩小', en: 'Zoom-' },
  { id: 'contrast', zh: '对比度', en: 'Contrast' }
]

const upperRow = ['18','17','16','15','14','13','12','11','21','22','23','24','25','26','27','28']
const lowerRow = ['48','47','46','45','44','43','42','41','31','32','33','34','35','36','37','38']

const lesionForTooth = (t: string) => LESIONS.find(l => l.tooth === t)
const randomNodes = Array.from({ length: 16 }, () => ({ left: 10 + Math.random()*80, top: 15 + Math.random()*70, delay: Math.random()*2 }))
</script>

<template>
  <div class="page page-analyze">
    <div class="ana-head">
      <div class="ana-breadcrumb">
        <span>{{ locale === 'zh' ? 'AI 分析' : 'AI Analysis' }}</span>
        <AppIcon name="chevron_right" :size="14" />
        <span class="mono">CA-20426-09</span>
        <AppIcon name="chevron_right" :size="14" />
        <span style="color: var(--ink-1)">{{ locale === 'zh' ? '王雪 · 32' : 'Wang Xue · 32' }}</span>
      </div>
      <div class="ana-head-actions">
        <span class="chip" style="background: var(--brand-100); color: var(--brand-800)"><AppIcon name="sparkle" :size="12" /> AI v2.4</span>
        <NButton size="small" secondary><template #icon><AppIcon name="compare" :size="14" /></template>{{ locale === 'zh' ? '历史对比' : 'Compare' }}</NButton>
        <NButton type="primary" size="small"><template #icon><AppIcon name="download" :size="14" /></template>{{ locale === 'zh' ? '导出 PDF' : 'Export PDF' }}</NButton>
      </div>
    </div>

    <div class="ana-body">
      <div class="ana-rail">
        <button v-for="t in tools" :key="t.id" class="ana-tool" :class="{ on: tool === t.id }" @click="toolClick(t.id)" :title="locale === 'zh' ? t.zh : t.en">
          <AppIcon :name="t.id" :size="16" />
        </button>
        <div class="ana-rail-sep"></div>
        <div class="ana-zoom mono">{{ Math.round(zoom * 100) }}%</div>
      </div>

      <div class="ana-canvas">
        <div class="ana-canvas-meta">
          <div class="mono">口内照 · Intraoral</div>
          <div class="mono">1920×1280 · 4.2MB</div>
        </div>

        <div class="ana-stage" :style="{ '--zoom': zoom } as any">
          <div class="ana-image-wrap">
            <ToothImage :scanning="phase === 'analyzing'" />

            <svg v-if="phase === 'result'" class="ana-overlay" viewBox="0 0 800 520">
              <g v-for="l in LESIONS" :key="l.id" :class="['lesion', `lesion-${l.sev}`, { active: l.id === activeLesion }, decisions[l.id]]" @click="activeLesion = l.id">
                <rect :x="l.x" :y="l.y" :width="l.w" :height="l.h" fill="transparent" :stroke="`var(--sev-${l.sev})`" :stroke-width="l.id === activeLesion ? 2.5 : 1.6" :stroke-dasharray="decisions[l.id] === 'rejected' ? '4 4' : 'none'" rx="3" />
                <rect :x="l.x" :y="l.y - 20" width="60" height="16" rx="3" :fill="`var(--sev-${l.sev})`" opacity=".96" />
                <text :x="l.x + 5" :y="l.y - 8" fill="#fff" font-size="10" font-family="JetBrains Mono" font-weight="600">{{ l.id }} · {{ Math.round(l.conf * 100) }}%</text>
              </g>
            </svg>

            <div v-if="phase === 'analyzing'" class="ana-scan-layer">
              <div class="ana-scan-beam"></div>
              <div class="ana-scan-grid"></div>
              <span v-for="(n, i) in randomNodes" :key="i" class="ana-scan-node" :style="{ left: n.left + '%', top: n.top + '%', animationDelay: n.delay + 's' }"></span>
            </div>

            <div v-if="phase === 'uploading'" class="ana-scan-layer">
              <div class="ana-up-ring" style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%)">
                <svg viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="44" fill="none" stroke="rgba(255,255,255,.15)" stroke-width="2" />
                  <circle cx="50" cy="50" r="44" fill="none" stroke="var(--brand-400)" stroke-width="2" :stroke-dasharray="`${progress * 2.76} 276`" stroke-linecap="round" transform="rotate(-90 50 50)" />
                </svg>
                <div class="ana-up-n mono">{{ Math.round(progress) }}%</div>
              </div>
            </div>
          </div>

          <div class="ana-canvas-status">
            <template v-if="phase === 'result'">
              <div class="acs-left">
                <span class="chip" style="background: var(--ok-100); color: var(--ok-700)"><AppIcon name="check" :size="11" /> {{ locale === 'zh' ? '分析完成' : 'Analysis complete' }}</span>
                <span class="mono">{{ LESIONS.length }} {{ locale === 'zh' ? '病灶' : 'lesions' }}</span>
                <span class="mono">0.82s</span>
              </div>
              <div class="acs-right"><button class="btn btn-subtle btn-sm" @click="runAnalyze"><AppIcon name="scan" :size="13" />{{ locale === 'zh' ? '重新分析' : 'Re-analyze' }}</button></div>
            </template>
            <template v-else-if="phase === 'analyzing'">
              <div class="acs-left"><span class="ana-pulse-dot"></span><span>{{ locale === 'zh' ? 'AI 正在分析影像' : 'Analyzing…' }}</span></div>
              <div class="acs-right mono">{{ Math.round(progress) }}%</div>
            </template>
            <template v-else-if="phase === 'uploading'">
              <div class="acs-left"><span>{{ locale === 'zh' ? '上传中…' : 'Uploading…' }}</span></div>
              <div class="acs-right mono">{{ Math.round(progress) }}%</div>
            </template>
          </div>

          <div v-if="phase === 'uploading' || phase === 'analyzing'" class="ana-progressbar"><div :style="{ width: progress + '%' }"></div></div>
        </div>
      </div>

      <aside class="ana-inspector">
        <div class="ana-ins-card">
          <div class="ana-ins-head">
            <div class="ana-ins-avatar">王</div>
            <div>
              <div class="ana-ins-name">王雪 Wang Xue</div>
              <div class="ana-ins-meta mono">♀ · 32 · CA-20426-09</div>
            </div>
          </div>
          <div class="ana-ins-pat-meta">
            <div><span class="micro">{{ locale === 'zh' ? '主诉' : 'Chief' }}</span><div>{{ locale === 'zh' ? '右上后牙冷热敏感 3 周' : 'Thermal sensitivity 3 wks' }}</div></div>
            <div><span class="micro">{{ locale === 'zh' ? '既往' : 'History' }}</span><div>{{ locale === 'zh' ? '吸烟 · 正畸史' : 'Smoker · Ortho' }}</div></div>
          </div>
        </div>

        <div class="ana-ins-card">
          <div class="ana-ins-section-head">
            <h4>{{ locale === 'zh' ? 'AI 检出病灶' : 'AI findings' }} · {{ LESIONS.length }}</h4>
          </div>
          <ul class="ana-lesion-list">
            <li v-for="l in LESIONS" :key="l.id" :class="[{ on: activeLesion === l.id }, decisions[l.id]]" @click="activeLesion = l.id">
              <span :class="`al-sev al-sev-${l.sev}`"></span>
              <div class="al-main">
                <div class="al-row-1"><span class="mono al-id">{{ l.id }}</span><span class="al-tooth">{{ l.tooth }} · {{ l.surface }}</span></div>
                <div class="al-row-2"><span>{{ sevLabel(l.sev) }}</span><span class="mono" style="color: var(--ink-3)">{{ Math.round(l.conf * 100) }}%</span></div>
              </div>
              <AppIcon v-if="decisions[l.id] === 'accepted'" name="check" :size="14" style="color: var(--ok-500)" />
              <AppIcon v-if="decisions[l.id] === 'rejected'" name="x" :size="14" style="color: var(--danger-500)" />
            </li>
          </ul>
        </div>

        <div class="ana-ins-card ana-ins-detail">
          <div class="ana-ins-section-head">
            <h4><span :class="`al-sev al-sev-${active.sev}`" style="margin-right: 8px"></span>{{ active.id }} · {{ active.tooth }} {{ active.surface }}</h4>
            <span class="chip" style="background: var(--brand-100); color: var(--brand-800)"><AppIcon name="sparkle" :size="11" />AI</span>
          </div>

          <div class="ana-detail-grid">
            <div><span class="micro">{{ locale === 'zh' ? '病变程度' : 'Severity' }}</span><div :class="`chip chip-sev-${active.sev}`" style="margin-top: 4px">{{ sevLabel(active.sev) }}</div></div>
            <div><span class="micro">{{ locale === 'zh' ? '置信度' : 'Confidence' }}</span><div class="ana-confbar"><div :style="{ width: (active.conf * 100) + '%' }"></div><span class="mono">{{ Math.round(active.conf * 100) }}%</span></div></div>
            <div><span class="micro">{{ locale === 'zh' ? '病灶大小' : 'Size' }}</span><div class="mono">{{ active.size }}</div></div>
            <div><span class="micro">{{ locale === 'zh' ? '牙面' : 'Surface' }}</span><div>{{ active.surface }}</div></div>
          </div>

          <div class="ana-rec">
            <div class="micro">{{ locale === 'zh' ? '治疗建议' : 'Recommendation' }}</div>
            <div class="ana-rec-text">{{ active.rec }}</div>
          </div>

          <div class="ana-decision">
            <button :class="['ana-dbtn', { accepted: decisions[active.id] === 'accepted' }]" @click="decisions[active.id] = 'accepted'">
              <AppIcon name="check" :size="14" />{{ locale === 'zh' ? '采纳' : 'Accept' }}
            </button>
            <button :class="['ana-dbtn', { rejected: decisions[active.id] === 'rejected' }]" @click="decisions[active.id] = 'rejected'">
              <AppIcon name="x" :size="14" />{{ locale === 'zh' ? '拒绝' : 'Reject' }}
            </button>
            <button class="ana-dbtn"><AppIcon name="pen" :size="14" />{{ locale === 'zh' ? '修改' : 'Edit' }}</button>
          </div>

          <div class="ana-tooth-chart">
            <div class="atc-head">
              <span class="micro">{{ locale === 'zh' ? '牙位图 · FDI' : 'Tooth chart · FDI' }}</span>
              <span class="mono" style="font-size: 11px; color: var(--ink-3)">{{ active.tooth }}</span>
            </div>
            <div class="atc-grid">
              <div class="atc-row">
                <button v-for="t in upperRow" :key="t" :class="['atc-tooth', lesionForTooth(t) ? `sev-${lesionForTooth(t)!.sev}` : '', { on: active.tooth === t }]" @click="{ const les = lesionForTooth(t); if (les) activeLesion = les.id }">
                  <span class="atc-n">{{ t }}</span>
                </button>
              </div>
              <div class="atc-row">
                <button v-for="t in lowerRow" :key="t" :class="['atc-tooth', lesionForTooth(t) ? `sev-${lesionForTooth(t)!.sev}` : '', { on: active.tooth === t }]" @click="{ const les = lesionForTooth(t); if (les) activeLesion = les.id }">
                  <span class="atc-n">{{ t }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
