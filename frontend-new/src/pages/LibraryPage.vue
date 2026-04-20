<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppIcon from '@/components/AppIcon.vue'
import ToothImage from '@/components/ToothImage.vue'

const { locale } = useI18n()
const filter = ref('all')
const view = ref<'grid' | 'list'>('grid')

const cases = [
  { id: 'CA-20426-09', name: '王雪', tooth: '11, 16', sev: 4, findings: 4, date: '今日 09:24', modality: 'IO', tag: 'root_canal' },
  { id: 'CA-20426-08', name: '李安', tooth: '46', sev: 3, findings: 2, date: '今日 08:50', modality: 'PA', tag: 'deep' },
  { id: 'CA-20425-14', name: '周明', tooth: '26 MO', sev: 4, findings: 1, date: '昨日 17:30', modality: 'PX', tag: 'flag' },
  { id: 'CA-20425-11', name: '吴芳', tooth: '17, 27', sev: 1, findings: 3, date: '昨日 15:12', modality: 'IO', tag: 'initial' },
  { id: 'CA-20425-07', name: '张伟', tooth: '14, 24', sev: 2, findings: 2, date: '昨日 11:40', modality: 'IO', tag: 'moderate' },
  { id: 'CA-20424-22', name: '赵琴', tooth: '36', sev: 3, findings: 1, date: '4 月 19', modality: 'PA', tag: 'deep' },
  { id: 'CA-20424-18', name: '孙浩', tooth: '46 MOD', sev: 2, findings: 2, date: '4 月 19', modality: 'IO', tag: 'moderate' },
  { id: 'CA-20423-05', name: '钱立', tooth: '25', sev: 1, findings: 1, date: '4 月 18', modality: 'IO', tag: 'initial' }
]

const filtered = computed(() => filter.value === 'all' ? cases : cases.filter(c => c.tag === filter.value))

const filters = [
  { k: 'all', zh: '全部', en: 'All', n: cases.length },
  { k: 'initial', zh: '浅龋', en: 'Initial', n: 2 },
  { k: 'moderate', zh: '中龋', en: 'Moderate', n: 2 },
  { k: 'deep', zh: '深龋', en: 'Deep', n: 3 },
  { k: 'root_canal', zh: '牙髓', en: 'Pulpal', n: 1 }
]
</script>

<template>
  <div class="page page-library">
    <div class="lib-head">
      <div>
        <h1>{{ locale === 'zh' ? '病例库' : 'Case library' }}</h1>
        <div class="lib-head-sub">{{ locale === 'zh' ? `共 ${cases.length} 例 · 支持筛选` : `${cases.length} cases` }}</div>
      </div>
      <div class="lib-head-actions">
        <div class="lib-search">
          <AppIcon name="search" :size="14" />
          <input :placeholder="locale === 'zh' ? '搜索病例 ID…' : 'Search…'" />
        </div>
      </div>
    </div>

    <div class="lib-filters">
      <div class="lib-filter-group">
        <button v-for="f in filters" :key="f.k" class="lib-filter" :class="{ on: filter === f.k }" @click="filter = f.k">
          {{ locale === 'zh' ? f.zh : f.en }} <span class="mono">{{ f.n }}</span>
        </button>
      </div>
      <div class="lib-viewswitch">
        <button :class="{ on: view === 'grid' }" @click="view = 'grid'"><AppIcon name="rect" :size="14" /></button>
        <button :class="{ on: view === 'list' }" @click="view = 'list'"><AppIcon name="menu" :size="14" /></button>
      </div>
    </div>

    <div v-if="view === 'grid'" class="lib-grid">
      <article v-for="c in filtered" :key="c.id" class="lib-card">
        <div class="lib-card-img">
          <ToothImage />
          <svg class="lib-card-overlay" viewBox="0 0 800 520">
            <rect x="300" y="180" width="60" height="60" fill="transparent" :stroke="`var(--sev-${c.sev})`" stroke-width="2" rx="3" />
          </svg>
          <div class="lib-card-tags">
            <span class="mono" style="background: rgba(0,0,0,.65); color: #fff; padding: 3px 6px; border-radius: 4px; font-size: 10px">{{ c.modality }}</span>
            <span :class="`chip chip-sev-${c.sev}`">{{ ['','浅龋','中龋','深龋','牙髓'][c.sev] }}</span>
          </div>
        </div>
        <div class="lib-card-body">
          <div class="lib-card-row">
            <span class="mono" style="color: var(--ink-3); font-size: 11px">{{ c.id }}</span>
            <span class="mono" style="color: var(--ink-4); font-size: 11px">{{ c.date }}</span>
          </div>
          <div class="lib-card-name">{{ c.name }}</div>
          <div class="lib-card-tooth">{{ locale === 'zh' ? '牙位' : 'Tooth' }} {{ c.tooth }} · {{ c.findings }} {{ locale === 'zh' ? '病灶' : 'lesion' }}</div>
        </div>
      </article>
    </div>

    <table v-else class="data-table lib-table">
      <thead>
        <tr><th>{{ locale === 'zh' ? '病例' : 'Case' }}</th><th>{{ locale === 'zh' ? '患者' : 'Patient' }}</th><th>{{ locale === 'zh' ? '牙位' : 'Tooth' }}</th><th>{{ locale === 'zh' ? '程度' : 'Severity' }}</th><th>{{ locale === 'zh' ? '模态' : 'Modality' }}</th><th>{{ locale === 'zh' ? '时间' : 'Date' }}</th></tr>
      </thead>
      <tbody>
        <tr v-for="c in filtered" :key="c.id">
          <td><span class="mono" style="color: var(--ink-3)">{{ c.id }}</span></td>
          <td><b>{{ c.name }}</b></td>
          <td>{{ c.tooth }}</td>
          <td><span :class="`chip chip-sev-${c.sev}`">{{ ['','浅龋','中龋','深龋','牙髓'][c.sev] }}</span></td>
          <td><span class="mono">{{ c.modality }}</span></td>
          <td class="mono" style="color: var(--ink-3)">{{ c.date }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
