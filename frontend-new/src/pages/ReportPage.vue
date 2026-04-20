<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NButton } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import ToothImage from '@/components/ToothImage.vue'

const { locale } = useI18n()

const findings = [
  { id: 'L1', tooth: '16', surface: 'O',  sev: 3, size: '3.1 mm', rec: '树脂充填 · Composite restoration', icdas: '5', duration: '45 min', cost: '¥ 960' },
  { id: 'L2', tooth: '26', surface: 'DO', sev: 2, size: '1.8 mm', rec: '窝沟封闭 · Sealant',                icdas: '3', duration: '25 min', cost: '¥ 480' },
  { id: 'L3', tooth: '14', surface: 'M',  sev: 1, size: '0.9 mm', rec: '观察 + 氟化 · Observe + fluoride',   icdas: '2', duration: '10 min', cost: '¥ 120' },
  { id: 'L4', tooth: '11', surface: 'L',  sev: 4, size: '5.4 mm', rec: '根管治疗 · Root canal therapy',     icdas: '6', duration: '90 min', cost: '¥ 2,400' }
]

const overlays = [
  { id: 'L1', x: 225, y: 185, w: 48, h: 44, sev: 3 },
  { id: 'L2', x: 425, y: 175, w: 42, h: 50, sev: 2 },
  { id: 'L3', x: 315, y: 200, w: 28, h: 36, sev: 1 },
  { id: 'L4', x: 540, y: 190, w: 38, h: 48, sev: 4 }
]
</script>

<template>
  <div class="page page-report">
    <div class="rpt-toolbar">
      <div class="rpt-crumbs">
        <span>{{ locale === 'zh' ? '诊断报告' : 'Reports' }}</span>
        <AppIcon name="chevron_right" :size="14" />
        <span class="mono">RP-20426-09</span>
      </div>
      <div class="rpt-actions">
        <span class="chip" style="background: var(--ok-100); color: var(--ok-700)"><AppIcon name="check" :size="11" />{{ locale === 'zh' ? '已签发' : 'Signed' }}</span>
        <NButton size="small" secondary><template #icon><AppIcon name="share" :size="14" /></template>{{ locale === 'zh' ? '分享' : 'Share' }}</NButton>
        <NButton type="primary" size="small"><template #icon><AppIcon name="download" :size="14" /></template>{{ locale === 'zh' ? '导出 PDF' : 'Export PDF' }}</NButton>
      </div>
    </div>

    <div class="rpt-doc">
      <header class="rpt-doc-head">
        <div>
          <div class="micro">{{ locale === 'zh' ? '诊断报告 / 辅助参考' : 'Diagnostic Report' }}</div>
          <h1>{{ locale === 'zh' ? '龋齿影像学分析报告' : 'Caries Imaging Analysis' }}</h1>
          <div class="rpt-head-meta">
            <span class="mono">RP-20426-09</span>
            <span>·</span>
            <span>{{ locale === 'zh' ? '2026 年 4 月 20 日 09:24' : 'Apr 20, 2026 · 09:24' }}</span>
          </div>
        </div>
        <div class="rpt-hospital">
          <div class="rpt-hos-logo"><AppIcon name="logo" :size="20" /></div>
          <div>
            <div style="font-weight: 600">{{ locale === 'zh' ? '北京口腔医院' : 'Beijing Stomatology' }}</div>
            <div class="mono" style="font-size: 11px; color: var(--ink-3)">{{ locale === 'zh' ? '修复科 · 3F 302' : 'Restorative · 3F 302' }}</div>
          </div>
        </div>
      </header>

      <div class="rpt-info-grid">
        <div class="rpt-info"><span class="micro">{{ locale === 'zh' ? '患者' : 'Patient' }}</span><div class="rpt-info-v">王雪 Wang Xue</div><div class="rpt-info-s mono">♀ · 32 · CA-20426-09</div></div>
        <div class="rpt-info"><span class="micro">{{ locale === 'zh' ? '主诉' : 'Chief' }}</span><div>{{ locale === 'zh' ? '右上后牙冷热敏感 3 周' : 'Thermal sensitivity 3 wks' }}</div></div>
        <div class="rpt-info"><span class="micro">{{ locale === 'zh' ? '影像模态' : 'Modality' }}</span><div>{{ locale === 'zh' ? '口内照 × 2 · 根尖片 × 1' : 'Intraoral × 2 · PA × 1' }}</div></div>
        <div class="rpt-info"><span class="micro">{{ locale === 'zh' ? 'AI 模型' : 'AI model' }}</span><div class="mono">DentAI-Caries v2.4</div></div>
      </div>

      <section class="rpt-section">
        <h2><span>01</span>{{ locale === 'zh' ? '影像概览' : 'Imaging overview' }}</h2>
        <div class="rpt-img-grid">
          <div class="rpt-img-main">
            <ToothImage />
            <svg class="rpt-img-overlay" viewBox="0 0 800 520">
              <g v-for="l in overlays" :key="l.id">
                <rect :x="l.x" :y="l.y" :width="l.w" :height="l.h" fill="transparent" :stroke="`var(--sev-${l.sev})`" stroke-width="1.8" rx="3" />
                <text :x="l.x + 4" :y="l.y - 6" :fill="`var(--sev-${l.sev})`" font-size="11" font-family="JetBrains Mono" font-weight="700">{{ l.id }}</text>
              </g>
            </svg>
          </div>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>02</span>{{ locale === 'zh' ? '诊断摘要' : 'Summary' }}</h2>
        <div class="rpt-kpis">
          <div class="rpt-kpi"><div class="rpt-kpi-n">4</div><div class="micro">{{ locale === 'zh' ? '检出病灶' : 'Lesions' }}</div></div>
          <div class="rpt-kpi"><div class="rpt-kpi-n">1</div><div class="micro">{{ locale === 'zh' ? '牙髓级' : 'Pulpal' }}</div></div>
          <div class="rpt-kpi"><div class="rpt-kpi-n">1</div><div class="micro">{{ locale === 'zh' ? '深龋' : 'Deep' }}</div></div>
          <div class="rpt-kpi"><div class="rpt-kpi-n">94<span>%</span></div><div class="micro">{{ locale === 'zh' ? '平均置信' : 'Avg conf' }}</div></div>
          <div class="rpt-kpi"><div class="rpt-kpi-n">170<span>min</span></div><div class="micro">{{ locale === 'zh' ? '预计时长' : 'Time' }}</div></div>
          <div class="rpt-kpi"><div class="rpt-kpi-n">¥3,960</div><div class="micro">{{ locale === 'zh' ? '预计费用' : 'Cost' }}</div></div>
        </div>
      </section>

      <section class="rpt-section">
        <h2><span>03</span>{{ locale === 'zh' ? '病灶清单' : 'Findings' }}</h2>
        <table class="rpt-table">
          <thead>
            <tr><th>#</th><th>{{ locale === 'zh' ? '牙位' : 'Tooth' }}</th><th>{{ locale === 'zh' ? '牙面' : 'Surface' }}</th><th>ICDAS</th><th>{{ locale === 'zh' ? '程度' : 'Severity' }}</th><th>{{ locale === 'zh' ? '大小' : 'Size' }}</th><th>{{ locale === 'zh' ? '建议' : 'Recommendation' }}</th><th>{{ locale === 'zh' ? '时长' : 'Time' }}</th><th>{{ locale === 'zh' ? '费用' : 'Cost' }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="f in findings" :key="f.id">
              <td><span class="mono" style="color: var(--ink-3)">{{ f.id }}</span></td>
              <td><b>{{ f.tooth }}</b></td>
              <td>{{ f.surface }}</td>
              <td class="mono">{{ f.icdas }}</td>
              <td><span :class="`chip chip-sev-${f.sev}`">{{ ['','浅','中','深','髓'][f.sev] }}</span></td>
              <td class="mono">{{ f.size }}</td>
              <td>{{ f.rec }}</td>
              <td class="mono">{{ f.duration }}</td>
              <td class="mono">{{ f.cost }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>
