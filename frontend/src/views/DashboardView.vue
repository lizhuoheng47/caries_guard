<template>
  <div class="dashboard">
    <!-- Header -->
    <header class="page-header">
      <h1>工作台</h1>
      <div class="header-actions">
        <button class="btn btn-ghost">
          <svg viewBox="0 0 18 18" fill="none">
            <path d="M9 2v9m0 0l-3-3m3 3l3-3M3 14v1.5A1.5 1.5 0 0 0 4.5 17h9a1.5 1.5 0 0 0 1.5-1.5V14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>导出数据</span>
        </button>
        <button class="btn btn-primary">
          <svg viewBox="0 0 18 18" fill="none">
            <path d="M9 4v10M4 9h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <span>新建诊断</span>
        </button>
      </div>
    </header>

    <!-- Stat cards -->
    <div class="stat-grid">
      <div v-for="s in stats" :key="s.key" class="stat-card" :class="`stat-${s.tone}`">
        <div class="stat-icon">
          <span v-html="s.icon"></span>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">
            <span class="num">{{ s.value }}</span>
            <span class="unit">{{ s.unit }}</span>
          </div>
          <div class="stat-trend">
            较昨日
            <span class="up">↑{{ s.trend }}</span>
          </div>
        </div>
        <div class="stat-bar"></div>
      </div>
    </div>

    <!-- Charts row -->
    <div class="row">
      <div class="card chart-card">
        <div class="card-head">
          <div class="card-title">
            <span class="title-bar"></span>
            诊断趋势
          </div>
          <div class="seg">
            <button :class="{ active: range === '7' }" @click="range = '7'">近7天</button>
            <button :class="{ active: range === '30' }" @click="range = '30'">近30天</button>
          </div>
        </div>

        <!-- Trend SVG -->
        <div class="trend-wrap">
          <svg :viewBox="`0 0 ${trendW} ${trendH}`" preserveAspectRatio="none" class="trend-svg">
            <defs>
              <linearGradient id="trendGradA" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#2ee6c8" stop-opacity="0.45"/>
                <stop offset="100%" stop-color="#2ee6c8" stop-opacity="0"/>
              </linearGradient>
              <linearGradient id="trendGradB" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#5eead4" stop-opacity="0.25"/>
                <stop offset="100%" stop-color="#5eead4" stop-opacity="0"/>
              </linearGradient>
            </defs>

            <!-- Y grid -->
            <g class="grid-lines">
              <line v-for="(y, i) in yTicks" :key="i" :x1="40" :x2="trendW - 8" :y1="y.y" :y2="y.y"/>
            </g>
            <g class="grid-labels">
              <text v-for="(y, i) in yTicks" :key="i" :x="32" :y="y.y + 4" text-anchor="end">{{ y.label }}</text>
            </g>

            <!-- Areas -->
            <path :d="areaA" fill="url(#trendGradA)"/>
            <path :d="areaB" fill="url(#trendGradB)"/>

            <!-- Lines -->
            <path :d="lineA" stroke="#2ee6c8" stroke-width="2" fill="none" stroke-linejoin="round" filter="url(#glowA)"/>
            <path :d="lineB" stroke="#5eead4" stroke-width="1.6" fill="none" stroke-linejoin="round" stroke-dasharray="3 3" opacity="0.7"/>

            <filter id="glowA" x="-5%" y="-5%" width="110%" height="120%">
              <feGaussianBlur stdDeviation="2" result="b"/>
              <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>

            <!-- Dots A -->
            <g>
              <circle v-for="(p, i) in pointsA" :key="i" :cx="p.x" :cy="p.y" r="3.5" fill="#0b242c" stroke="#2ee6c8" stroke-width="1.8"/>
            </g>

            <!-- X labels -->
            <g class="grid-labels">
              <text v-for="(d, i) in trend.dates" :key="i" :x="xAt(i)" :y="trendH - 6" text-anchor="middle">{{ d }}</text>
            </g>
          </svg>
        </div>
        <div class="legend">
          <span class="dot dot-a"></span> 扫描数量
          <span class="dot dot-b" style="margin-left:18px"></span> 诊断数量
        </div>
      </div>

      <div class="card donut-card">
        <div class="card-head">
          <div class="card-title">
            <span class="title-bar"></span>
            病例分布
          </div>
        </div>

        <div class="donut-wrap">
          <svg viewBox="0 0 220 220" class="donut-svg">
            <defs>
              <filter id="donutGlow" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="3" result="b"/>
                <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
            </defs>
            <g transform="translate(110 110)" filter="url(#donutGlow)">
              <circle r="78" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="22"/>
              <circle
                v-for="(seg, i) in donutSegs"
                :key="i"
                r="78"
                fill="none"
                :stroke="seg.color"
                stroke-width="22"
                :stroke-dasharray="`${seg.len} ${donutCirc}`"
                :stroke-dashoffset="seg.offset"
                stroke-linecap="butt"
                transform="rotate(-90)"
              />
            </g>
            <text x="110" y="106" text-anchor="middle" class="donut-num">1,234</text>
            <text x="110" y="128" text-anchor="middle" class="donut-cap">总数</text>
          </svg>

          <div class="donut-legend">
            <div v-for="(seg, i) in donutData" :key="i" class="legend-row">
              <span class="legend-dot" :style="{ background: seg.color }"></span>
              <span class="legend-name">{{ seg.name }}</span>
              <span class="legend-val">{{ seg.pct }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent cases -->
    <div class="card">
      <div class="card-head">
        <div class="card-title">
          <span class="title-bar"></span>
          最近病例
        </div>
      </div>
      <table class="case-table">
        <thead>
          <tr>
            <th>患者信息</th>
            <th>检查类型</th>
            <th>AI诊断结果</th>
            <th>风险等级</th>
            <th>检查时间</th>
            <th class="op-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in cases" :key="i">
            <td class="patient">{{ r.patient }}</td>
            <td>{{ r.type }}</td>
            <td>{{ r.result }}</td>
            <td>
              <span class="risk-tag" :class="`risk-${r.riskLevel}`">{{ r.risk }}</span>
            </td>
            <td>{{ r.time }}</td>
            <td>
              <button class="row-btn" aria-label="查看">
                <svg viewBox="0 0 16 16" fill="none">
                  <path d="M3 13L13 3M13 3H6M13 3v7" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const range = ref<'7' | '30'>('7')

const stats = [
  {
    key: 'scan',
    label: '总扫描数量',
    value: '1,234',
    unit: '例',
    trend: '15%',
    tone: 'mint',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M4 16v2a2 2 0 0 0 2 2h2M16 20h2a2 2 0 0 0 2-2v-2M8 12h8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'ai',
    label: 'AI诊断数量',
    value: '987',
    unit: '例',
    trend: '10%',
    tone: 'violet',
    icon: `<svg viewBox="0 0 24 24" fill="none"><rect x="4" y="6" width="16" height="13" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="M9 11h.01M15 11h.01M9 15c1.2.8 4.8.8 6 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M9 6V4M15 6V4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'risk',
    label: '高风险病例',
    value: '89',
    unit: '例',
    trend: '8%',
    tone: 'rose',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l9 16H3l9-16Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M12 10v4M12 17h.01" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'acc',
    label: '诊断准确率',
    value: '96.7',
    unit: '%',
    trend: '2.3%',
    tone: 'mint',
    icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.7"/><path d="M8 12l3 3 5-6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  },
]

const cases = [
  { patient: '王** 男 / 28岁', type: '全景片', result: '多颗牙龋坏，建议充填治疗', risk: '高风险', riskLevel: 'high', time: '2024-06-01 14:30' },
  { patient: '李** 女 / 35岁', type: '口内片', result: '右下智齿阻生，建议拔除', risk: '中风险', riskLevel: 'mid', time: '2024-06-01 13:45' },
  { patient: '张** 男 / 42岁', type: 'CBCT', result: '左上颌窦炎伴骨质吸收', risk: '高风险', riskLevel: 'high', time: '2024-06-01 11:20' },
  { patient: '刘** 女 / 31岁', type: '全景片', result: '牙周轻度骨吸收', risk: '低风险', riskLevel: 'low', time: '2024-06-01 10:15' },
]

/* ===== Trend chart math ===== */
const trendW = 720
const trendH = 250
const trend = {
  dates: ['05-25', '05-26', '05-27', '05-28', '05-30', '05-31', '06-01'],
  scan: [380, 620, 780, 920, 1100, 1280, 980],
  diag: [220, 470, 600, 760, 880, 1000, 800],
}
const yMax = 1500
const yTicks = computed(() => {
  const ticks = [1500, 1200, 900, 600, 300, 0]
  const top = 16
  const bot = trendH - 26
  return ticks.map(t => ({
    label: t.toLocaleString(),
    y: top + (bot - top) * (1 - t / yMax),
  }))
})
const xAt = (i: number) => 50 + i * ((trendW - 70) / (trend.dates.length - 1))
const yAt = (v: number) => {
  const top = 16
  const bot = trendH - 26
  return top + (bot - top) * (1 - v / yMax)
}
const pointsA = computed(() => trend.scan.map((v, i) => ({ x: xAt(i), y: yAt(v) })))
const pointsB = computed(() => trend.diag.map((v, i) => ({ x: xAt(i), y: yAt(v) })))

const smooth = (pts: { x: number; y: number }[]) => {
  if (pts.length < 2) return ''
  let d = `M ${pts[0].x} ${pts[0].y}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i]
    const p1 = pts[i + 1]
    const cx = (p0.x + p1.x) / 2
    d += ` C ${cx} ${p0.y}, ${cx} ${p1.y}, ${p1.x} ${p1.y}`
  }
  return d
}
const lineA = computed(() => smooth(pointsA.value))
const lineB = computed(() => smooth(pointsB.value))
const areaA = computed(() => `${lineA.value} L ${pointsA.value[pointsA.value.length - 1].x} ${trendH - 26} L ${pointsA.value[0].x} ${trendH - 26} Z`)
const areaB = computed(() => `${lineB.value} L ${pointsB.value[pointsB.value.length - 1].x} ${trendH - 26} L ${pointsB.value[0].x} ${trendH - 26} Z`)

/* ===== Donut math ===== */
const donutData = [
  { name: '龋齿', pct: 45, color: '#2ee6c8' },
  { name: '牙周病', pct: 25, color: '#9b6bff' },
  { name: '根尖周病', pct: 20, color: '#f7a23a' },
  { name: '其他', pct: 10, color: '#5eead4' },
]
const donutCirc = 2 * Math.PI * 78
const donutSegs = computed(() => {
  let acc = 0
  return donutData.map(d => {
    const len = (d.pct / 100) * donutCirc
    const seg = { color: d.color, len, offset: -acc }
    acc += len
    return seg
  })
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.page-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #f5fffb;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid rgba(94, 234, 212, 0.22);
  background: rgba(11, 36, 44, 0.65);
  color: #e8fff8;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.btn svg { width: 16px; height: 16px; }
.btn:hover { border-color: rgba(94, 234, 212, 0.55); transform: translateY(-1px); }
.btn-ghost { color: #9bc6bd; }

.btn-primary {
  border-color: rgba(46, 230, 200, 0.6);
  background: linear-gradient(135deg, rgba(46, 230, 200, 0.22), rgba(46, 230, 200, 0.08));
  color: #2ee6c8;
  box-shadow: 0 0 18px rgba(46, 230, 200, 0.18), inset 0 0 18px rgba(46, 230, 200, 0.05);
}

/* Stat cards */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat-card {
  position: relative;
  padding: 18px 20px 18px 20px;
  border-radius: 14px;
  border: 1px solid rgba(94, 234, 212, 0.1);
  background:
    linear-gradient(180deg, rgba(13, 38, 47, 0.7), rgba(8, 23, 28, 0.85));
  display: flex;
  gap: 14px;
  overflow: hidden;
}

.stat-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent-line, rgba(46, 230, 200, 0.45)), transparent);
}

.stat-mint { --accent-line: rgba(46, 230, 200, 0.55); --accent-color: #2ee6c8; }
.stat-violet { --accent-line: rgba(155, 107, 255, 0.55); --accent-color: #9b6bff; }
.stat-rose { --accent-line: rgba(255, 99, 110, 0.55); --accent-color: #ff636e; }

.stat-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(0,0,0,0.25);
  border: 1px solid color-mix(in oklab, var(--accent-color, #2ee6c8) 35%, transparent);
  color: var(--accent-color, #2ee6c8);
  filter: drop-shadow(0 0 8px color-mix(in oklab, var(--accent-color, #2ee6c8) 35%, transparent));
}

.stat-icon svg,
.stat-icon :deep(svg) { width: 20px; height: 20px; }

.stat-body { flex: 1; min-width: 0; }
.stat-label { font-size: 12px; color: #9bc6bd; letter-spacing: 1px; }

.stat-value {
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-value .num {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #f5fffb;
  text-shadow: 0 0 14px color-mix(in oklab, var(--accent-color, #2ee6c8) 40%, transparent);
}

.stat-value .unit { font-size: 13px; color: #9bc6bd; }

.stat-trend {
  margin-top: 6px;
  font-size: 11px;
  color: #5e8a82;
}

.stat-trend .up { color: var(--accent-color, #2ee6c8); margin-left: 4px; }

/* Card baseline */
.card {
  position: relative;
  border-radius: 14px;
  border: 1px solid rgba(94, 234, 212, 0.1);
  background: linear-gradient(180deg, rgba(13, 38, 47, 0.65), rgba(8, 23, 28, 0.85));
  padding: 18px 20px;
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
  background: var(--accent);
  box-shadow: 0 0 8px var(--accent);
}

.seg {
  display: flex;
  border-radius: 8px;
  border: 1px solid rgba(94, 234, 212, 0.15);
  padding: 2px;
  background: rgba(0,0,0,0.25);
}

.seg button {
  border: 0;
  background: transparent;
  color: #9bc6bd;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.seg button.active {
  background: rgba(46, 230, 200, 0.18);
  color: #2ee6c8;
}

/* Row */
.row {
  display: grid;
  grid-template-columns: 1.85fr 1fr;
  gap: 14px;
}

/* Trend */
.trend-wrap { width: 100%; height: 250px; }
.trend-svg { width: 100%; height: 100%; }
.grid-lines line {
  stroke: rgba(94, 234, 212, 0.06);
  stroke-width: 1;
}
.grid-labels text {
  fill: #5e8a82;
  font-size: 10px;
  font-family: inherit;
}

.legend {
  text-align: center;
  font-size: 11px;
  color: #9bc6bd;
  margin-top: 4px;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 2px;
  vertical-align: middle;
  margin-right: 4px;
}

.dot-a { background: #2ee6c8; box-shadow: 0 0 4px #2ee6c8; }
.dot-b { background: #5eead4; opacity: 0.7; }

/* Donut */
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.donut-svg {
  width: 200px;
  height: 200px;
  flex-shrink: 0;
}

.donut-num {
  fill: #f5fffb;
  font-size: 24px;
  font-weight: 800;
}

.donut-cap {
  fill: #9bc6bd;
  font-size: 11px;
}

.donut-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #c5e2dc;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}

.legend-name { flex: 1; }
.legend-val { color: #f5fffb; font-weight: 600; }

/* Cases table */
.case-table {
  width: 100%;
  border-collapse: collapse;
}

.case-table th,
.case-table td {
  text-align: left;
  padding: 12px 8px;
  font-size: 12px;
}

.case-table th {
  color: #5e8a82;
  font-weight: 500;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(94, 234, 212, 0.08);
}

.case-table tbody tr {
  border-bottom: 1px solid rgba(94, 234, 212, 0.04);
  transition: background 0.18s ease;
}

.case-table tbody tr:hover {
  background: rgba(46, 230, 200, 0.03);
}

.case-table td { color: #c5e2dc; }
.case-table td.patient { color: #e8fff8; font-weight: 600; }

.risk-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.risk-high {
  color: #ff636e;
  background: rgba(255, 99, 110, 0.12);
  border: 1px solid rgba(255, 99, 110, 0.4);
}

.risk-mid {
  color: #f7a23a;
  background: rgba(247, 162, 58, 0.12);
  border: 1px solid rgba(247, 162, 58, 0.4);
}

.risk-low {
  color: #2ee6c8;
  background: rgba(46, 230, 200, 0.12);
  border: 1px solid rgba(46, 230, 200, 0.4);
}

.op-col { width: 60px; }

.row-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid rgba(46, 230, 200, 0.3);
  background: rgba(46, 230, 200, 0.08);
  color: #2ee6c8;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.18s ease;
}

.row-btn:hover { background: rgba(46, 230, 200, 0.18); }
.row-btn svg { width: 14px; height: 14px; }
</style>
