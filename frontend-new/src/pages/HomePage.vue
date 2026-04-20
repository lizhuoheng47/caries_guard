<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NCard, NButton } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const { t, locale } = useI18n()

const sparkData = (arr: number[], color: string, height = 40) => {
  const w = 140, h = height
  const max = Math.max(...arr), min = Math.min(...arr)
  const pts = arr.map((v, i) => {
    const x = (i / (arr.length - 1)) * w
    const y = h - ((v - min) / (max - min || 1)) * (h - 4) - 2
    return `${x},${y}`
  }).join(' ')
  return { w, h, pts, color, areaPts: `0,${h} ${pts} ${w},${h}` }
}

const stats = computed(() => [
  { k: t('common.search').includes('搜') ? '待分析' : 'In queue', n: 12, d: '+3', spark: sparkData([3,5,4,6,7,9,12], 'var(--brand-500)') },
  { k: locale.value === 'zh' ? '已完成' : 'Completed', n: 48, d: '+8', spark: sparkData([32,36,39,40,44,46,48], 'var(--ok-500)') },
  { k: locale.value === 'zh' ? '需复核' : 'Flagged', n: 3, d: '-2', spark: sparkData([5,6,5,4,3,4,3], 'var(--warn-500)') },
  { k: locale.value === 'zh' ? '模型准确率' : 'Model accuracy', n: '98.2%', d: '+0.4', spark: sparkData([96,97,97,98,97,98,98], 'var(--info-500)') }
])

const cases = [
  { id: 'CA-20426-09', name: '王雪 Wang Xue', age: 32, time: '09:12', status: 'analyzing', severity: 3, findings: 4, tooth: '16, 26, 37, 46' },
  { id: 'CA-20426-08', name: '李安 Li An', age: 45, time: '08:50', status: 'done', severity: 2, findings: 2, tooth: '25, 35' },
  { id: 'CA-20425-14', name: '周明 Zhou M.', age: 28, time: '昨日', status: 'flagged', severity: 4, findings: 1, tooth: '46 MOD' },
  { id: 'CA-20425-11', name: '吴芳 Wu Fang', age: 52, time: '昨日', status: 'done', severity: 1, findings: 3, tooth: '17, 27, 47' },
  { id: 'CA-20425-07', name: '张伟 Zhang W.', age: 39, time: '昨日', status: 'done', severity: 2, findings: 2, tooth: '14, 24' }
]

const sevLabel = (s: number) => ['', '浅龋 Initial', '中龋 Moderate', '深龋 Deep', '牙髓 Pulpal'][s]
</script>

<template>
  <div class="page page-home">
    <div class="page-hello">
      <div>
        <div class="micro">{{ locale === 'zh' ? '早安 · 周一 · 4 月 20' : 'Good morning · Mon · Apr 20' }}</div>
        <h1 class="page-hello-title">{{ locale === 'zh' ? '陈医师，今天有 12 例待诊。' : 'Dr. Chen, 12 cases waiting today.' }}</h1>
      </div>
      <NButton type="primary" size="large" @click="router.push('/app/analyze')">
        <template #icon><AppIcon name="upload" :size="15" /></template>
        {{ locale === 'zh' ? '上传新影像' : 'Upload new image' }}
      </NButton>
    </div>

    <div class="home-stats">
      <div v-for="(s, i) in stats" :key="i" class="card home-stat">
        <div class="home-stat-head">
          <span class="micro">{{ s.k }}</span>
          <span class="home-stat-delta" :style="{ color: s.d.startsWith('+') ? 'var(--ok-700)' : 'var(--danger-700)' }">{{ s.d }}</span>
        </div>
        <div class="home-stat-row">
          <div class="home-stat-n">{{ s.n }}</div>
          <svg :width="s.spark.w" :height="s.spark.h" :viewBox="`0 0 ${s.spark.w} ${s.spark.h}`">
            <polygon :points="s.spark.areaPts" :fill="s.spark.color" opacity=".1" />
            <polyline :points="s.spark.pts" fill="none" :stroke="s.spark.color" stroke-width="1.5" stroke-linejoin="round" />
          </svg>
        </div>
      </div>
    </div>

    <div class="home-grid">
      <section class="card home-queue">
        <div class="card-head">
          <h3>{{ locale === 'zh' ? '今日病例队列' : "Today's case queue" }}</h3>
          <div class="card-head-actions">
            <button class="chip chip-neutral">{{ locale === 'zh' ? '全部' : 'All' }} · 12</button>
            <button class="chip">{{ locale === 'zh' ? '待分析' : 'Pending' }} · 3</button>
            <button class="chip chip-neutral">{{ locale === 'zh' ? '已完成' : 'Done' }} · 9</button>
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ locale === 'zh' ? '病例' : 'Case' }}</th>
              <th>{{ locale === 'zh' ? '患者' : 'Patient' }}</th>
              <th>{{ locale === 'zh' ? '时间' : 'Time' }}</th>
              <th>{{ locale === 'zh' ? '病灶' : 'Lesions' }}</th>
              <th>{{ locale === 'zh' ? '最重程度' : 'Max severity' }}</th>
              <th>{{ locale === 'zh' ? '状态' : 'Status' }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in cases" :key="c.id" @click="router.push('/app/analyze')">
              <td><span class="mono" style="color: var(--ink-3)">{{ c.id }}</span></td>
              <td>
                <div class="dt-patient">
                  <div class="dt-avatar">{{ c.name[0] }}</div>
                  <div>
                    <div>{{ c.name }}</div>
                    <div class="dt-sub">{{ c.age }} · {{ c.tooth }}</div>
                  </div>
                </div>
              </td>
              <td><span class="mono" style="font-size: 12px; color: var(--ink-3)">{{ c.time }}</span></td>
              <td><span class="mono">{{ c.findings }}</span></td>
              <td><span :class="`chip chip-sev-${c.severity}`">{{ sevLabel(c.severity) }}</span></td>
              <td>
                <span v-if="c.status === 'analyzing'" class="status status-analyzing"><span class="dot"></span>{{ locale === 'zh' ? '分析中' : 'Analyzing' }}</span>
                <span v-if="c.status === 'done'" class="status status-done"><AppIcon name="check" :size="11" />{{ locale === 'zh' ? '已完成' : 'Done' }}</span>
                <span v-if="c.status === 'flagged'" class="status status-flag"><AppIcon name="alert" :size="11" />{{ locale === 'zh' ? '需复核' : 'Review' }}</span>
              </td>
              <td><AppIcon name="chevron_right" :size="16" style="color: var(--ink-4)" /></td>
            </tr>
          </tbody>
        </table>
      </section>

      <aside class="home-side">
        <section class="card home-upload">
          <div class="card-head"><h3>{{ locale === 'zh' ? '快速分析' : 'Quick analysis' }}</h3></div>
          <button class="home-drop" @click="router.push('/app/analyze')">
            <AppIcon name="upload" :size="26" />
            <div class="home-drop-t">{{ t('common.dragDrop') }}</div>
            <div class="home-drop-s">{{ t('common.supported') }}</div>
          </button>
        </section>

        <section class="card home-recent">
          <div class="card-head"><h3>{{ locale === 'zh' ? '近期标注' : 'Recent annotations' }}</h3></div>
          <ul class="home-timeline">
            <li><div class="ht-dot" style="background: var(--sev-3)"></div><div><div>{{ locale === 'zh' ? '采纳 AI 建议：46 牙合面深龋' : 'Accepted: 46 deep caries' }}</div><div class="dt-sub mono">09:08 · CA-20426-08</div></div></li>
            <li><div class="ht-dot" style="background: var(--warn-500)"></div><div><div>{{ locale === 'zh' ? '修正病灶边界：25 邻面' : 'Adjusted: 25 mesial' }}</div><div class="dt-sub mono">08:44 · CA-20426-06</div></div></li>
            <li><div class="ht-dot" style="background: var(--ok-500)"></div><div><div>{{ locale === 'zh' ? '已签发报告 × 3' : 'Signed 3 reports' }}</div><div class="dt-sub mono">{{ locale === 'zh' ? '昨日 18:20' : 'Yesterday 18:20' }}</div></div></li>
            <li><div class="ht-dot" style="background: var(--info-500)"></div><div><div>{{ locale === 'zh' ? '模型更新至 v2.4' : 'Model v2.4' }}</div><div class="dt-sub mono">{{ locale === 'zh' ? '4 月 18 日' : 'Apr 18' }}</div></div></li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>
