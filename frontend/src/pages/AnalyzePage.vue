<script setup lang="ts">
import { computed, ref } from 'vue'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import AppIcon from '@/components/AppIcon.vue'
import ToothImage from '@/components/ToothImage.vue'

interface Lesion {
  id: string
  tooth: string
  surface: string
  x: number
  y: number
  w: number
  h: number
  sev: number
  conf: number
  size: string
  rec: string
}

const { locale } = useI18n()

const lesions: Lesion[] = [
  { id: 'L1', tooth: '16', surface: 'O', x: 225, y: 185, w: 48, h: 44, sev: 3, conf: 0.94, size: '3.1 mm', rec: '复合树脂修复' },
  { id: 'L2', tooth: '26', surface: 'DO', x: 425, y: 175, w: 42, h: 50, sev: 2, conf: 0.87, size: '1.8 mm', rec: '建议封闭与随访' },
  { id: 'L3', tooth: '14', surface: 'M', x: 315, y: 200, w: 28, h: 36, sev: 1, conf: 0.72, size: '0.9 mm', rec: '观察并氟化' }
]

const tools = [
  { id: 'move', label: 'Move' },
  { id: 'rect', label: 'Box' },
  { id: 'pen', label: 'Pen' },
  { id: 'zoom_in', label: 'Zoom+' },
  { id: 'zoom_out', label: 'Zoom-' },
  { id: 'contrast', label: 'Contrast' }
]

const activeLesion = ref('L1')
const tool = ref('move')
const zoom = ref(1)
const decisions = ref<Record<string, 'pending' | 'accepted' | 'rejected'>>({
  L1: 'pending',
  L2: 'pending',
  L3: 'pending'
})

const active = computed(() => lesions.find((item) => item.id === activeLesion.value) || lesions[0])

const sevLabel = (severity: number) => ['', '初期', '中度', '深龋', '牙髓'][severity] || '未知'

const toolClick = (id: string) => {
  tool.value = id
  if (id === 'zoom_in') zoom.value = Math.min(zoom.value + 0.2, 2)
  if (id === 'zoom_out') zoom.value = Math.max(zoom.value - 0.2, 0.6)
}
</script>

<template>
  <div class="page page-analyze">
    <div class="ana-head">
      <div class="ana-breadcrumb">
        <span>{{ locale === 'zh' ? 'AI 分析' : 'AI Analysis' }}</span>
        <AppIcon name="chevron_right" :size="14" />
        <span class="mono">CA-20426-09</span>
        <AppIcon name="chevron_right" :size="14" />
        <span style="color: var(--ink-1)">{{ locale === 'zh' ? '示例患者 · 口内影像' : 'Sample patient · Intraoral image' }}</span>
      </div>
      <div class="ana-head-actions">
        <span class="chip" style="background: var(--brand-100); color: var(--brand-800)">
          <AppIcon name="sparkle" :size="12" /> AI v1.0
        </span>
        <NButton size="small" secondary>
          <template #icon><AppIcon name="compare" :size="14" /></template>
          {{ locale === 'zh' ? '历史对比' : 'Compare' }}
        </NButton>
        <NButton type="primary" size="small">
          <template #icon><AppIcon name="download" :size="14" /></template>
          {{ locale === 'zh' ? '导出 PDF' : 'Export PDF' }}
        </NButton>
      </div>
    </div>

    <div class="ana-body">
      <div class="ana-rail">
        <button
          v-for="item in tools"
          :key="item.id"
          class="ana-tool"
          :class="{ on: tool === item.id }"
          :title="item.label"
          @click="toolClick(item.id)"
        >
          <AppIcon :name="item.id" :size="16" />
        </button>
        <div class="ana-rail-sep"></div>
        <div class="ana-zoom mono">{{ Math.round(zoom * 100) }}%</div>
      </div>

      <div class="ana-canvas">
        <div class="ana-canvas-meta">
          <div class="mono">Intraoral</div>
          <div class="mono">1920 × 1280 · 4.2MB</div>
        </div>

        <div class="ana-stage" :style="{ '--zoom': zoom }">
          <div class="ana-image-wrap">
            <ToothImage />
            <svg class="ana-overlay" viewBox="0 0 800 520">
              <g
                v-for="lesion in lesions"
                :key="lesion.id"
                :class="['lesion', `lesion-${lesion.sev}`, { active: lesion.id === activeLesion }, decisions[lesion.id]]"
                @click="activeLesion = lesion.id"
              >
                <rect :x="lesion.x" :y="lesion.y" :width="lesion.w" :height="lesion.h" fill="transparent" :stroke="`var(--sev-${lesion.sev})`" :stroke-width="lesion.id === activeLesion ? 2.5 : 1.6" rx="3" />
                <rect :x="lesion.x" :y="lesion.y - 20" width="60" height="16" rx="3" :fill="`var(--sev-${lesion.sev})`" opacity=".96" />
                <text :x="lesion.x + 5" :y="lesion.y - 8" fill="#fff" font-size="10" font-family="JetBrains Mono" font-weight="600">
                  {{ lesion.id }} · {{ Math.round(lesion.conf * 100) }}%
                </text>
              </g>
            </svg>
          </div>

          <div class="ana-canvas-status">
            <div class="acs-left">
              <span class="chip" style="background: var(--ok-100); color: var(--ok-700)">
                <AppIcon name="check" :size="11" />
                {{ locale === 'zh' ? '分析完成' : 'Analysis complete' }}
              </span>
              <span class="mono">{{ lesions.length }} {{ locale === 'zh' ? '处病灶' : 'lesions' }}</span>
              <span class="mono">0.82s</span>
            </div>
            <div class="acs-right">
              <button class="btn btn-subtle btn-sm">
                <AppIcon name="scan" :size="13" />{{ locale === 'zh' ? '重新分析' : 'Re-analyze' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <aside class="ana-inspector">
        <div class="ana-ins-card">
          <div class="ana-ins-head">
            <div class="ana-ins-avatar">示</div>
            <div>
              <div class="ana-ins-name">{{ locale === 'zh' ? '示例患者' : 'Sample patient' }}</div>
              <div class="ana-ins-meta mono">CA-20426-09</div>
            </div>
          </div>
          <div class="ana-ins-pat-meta">
            <div>
              <span class="micro">{{ locale === 'zh' ? '主诉' : 'Chief' }}</span>
              <div>{{ locale === 'zh' ? '后牙冷刺激敏感' : 'Posterior sensitivity' }}</div>
            </div>
            <div>
              <span class="micro">{{ locale === 'zh' ? '来源' : 'Source' }}</span>
              <div>{{ locale === 'zh' ? '口内影像' : 'Intraoral image' }}</div>
            </div>
          </div>
        </div>

        <div class="ana-ins-card">
          <div class="ana-ins-section-head">
            <h4>{{ locale === 'zh' ? 'AI 检出病灶' : 'AI findings' }} · {{ lesions.length }}</h4>
          </div>
          <ul class="ana-lesion-list">
            <li v-for="lesion in lesions" :key="lesion.id" :class="[{ on: activeLesion === lesion.id }, decisions[lesion.id]]" @click="activeLesion = lesion.id">
              <span :class="`al-sev al-sev-${lesion.sev}`"></span>
              <div class="al-main">
                <div class="al-row-1"><span class="mono al-id">{{ lesion.id }}</span><span class="al-tooth">{{ lesion.tooth }} · {{ lesion.surface }}</span></div>
                <div class="al-row-2"><span>{{ sevLabel(lesion.sev) }}</span><span class="mono">{{ Math.round(lesion.conf * 100) }}%</span></div>
              </div>
              <AppIcon v-if="decisions[lesion.id] === 'accepted'" name="check" :size="14" style="color: var(--ok-500)" />
              <AppIcon v-if="decisions[lesion.id] === 'rejected'" name="x" :size="14" style="color: var(--danger-500)" />
            </li>
          </ul>
        </div>

        <div class="ana-ins-card ana-ins-detail">
          <div class="ana-ins-section-head">
            <h4><span :class="`al-sev al-sev-${active.sev}`" style="margin-right: 8px"></span>{{ active.id }} · {{ active.tooth }} {{ active.surface }}</h4>
            <span class="chip" style="background: var(--brand-100); color: var(--brand-800)">
              <AppIcon name="sparkle" :size="11" />AI
            </span>
          </div>

          <div class="ana-detail-grid">
            <div>
              <span class="micro">{{ locale === 'zh' ? '病变程度' : 'Severity' }}</span>
              <div :class="`chip chip-sev-${active.sev}`" style="margin-top: 4px">{{ sevLabel(active.sev) }}</div>
            </div>
            <div>
              <span class="micro">{{ locale === 'zh' ? '置信度' : 'Confidence' }}</span>
              <div class="ana-confbar">
                <div :style="{ width: `${active.conf * 100}%` }"></div>
                <span class="mono">{{ Math.round(active.conf * 100) }}%</span>
              </div>
            </div>
            <div>
              <span class="micro">{{ locale === 'zh' ? '病灶大小' : 'Size' }}</span>
              <div class="mono">{{ active.size }}</div>
            </div>
            <div>
              <span class="micro">{{ locale === 'zh' ? '牙面' : 'Surface' }}</span>
              <div>{{ active.surface }}</div>
            </div>
          </div>

          <div class="ana-rec">
            <div class="micro">{{ locale === 'zh' ? '处理建议' : 'Recommendation' }}</div>
            <div class="ana-rec-text">{{ active.rec }}</div>
          </div>

          <div class="ana-decision">
            <button class="ana-dbtn" :class="{ accepted: decisions[active.id] === 'accepted' }" @click="decisions[active.id] = 'accepted'">
              <AppIcon name="check" :size="14" />{{ locale === 'zh' ? '采纳' : 'Accept' }}
            </button>
            <button class="ana-dbtn" :class="{ rejected: decisions[active.id] === 'rejected' }" @click="decisions[active.id] = 'rejected'">
              <AppIcon name="x" :size="14" />{{ locale === 'zh' ? '驳回' : 'Reject' }}
            </button>
            <button class="ana-dbtn">
              <AppIcon name="pen" :size="14" />{{ locale === 'zh' ? '修改' : 'Edit' }}
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
