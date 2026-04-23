<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import { useI18n } from 'vue-i18n'
import { setLang } from '@/i18n'
import { useAuthStore } from '@/stores/auth'

type NavId = 'home' | 'analyze' | 'cases'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t, locale } = useI18n()

const navItems = computed(() => [
  { id: 'home' as const, icon: 'home', label: t('nav.home'), path: '/dashboard/ai' },
  { id: 'analyze' as const, icon: 'scan', label: t('nav.analyze'), path: '/analysis' },
  { id: 'cases' as const, icon: 'settings', label: t('nav.cases'), path: '/cases' }
])

const activeId = computed<NavId>(() => {
  const path = route.path
  if (path.startsWith('/dashboard')) return 'home'
  if (path.startsWith('/analysis')) return 'analyze'
  return 'cases'
})

const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'Doctor')
const displayId = computed(() => authStore.user?.username || 'DR-0001')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())

const go = (path: string) => {
  router.push(path)
}

const toggleLang = () => {
  setLang(locale.value === 'zh' ? 'en' : 'zh')
}
</script>

<template>
  <div class="shell shell-classic">
    <aside class="shell-nav">
      <div class="shell-brand">
        <div class="shell-brand-mark"><AppIcon name="logo" :size="18" /></div>
        <div class="shell-brand-text">
          <div class="shell-brand-name">{{ t('product') }}</div>
          <div class="shell-brand-role">
            {{ locale === 'zh' ? '龋齿影像辅助工作台' : 'Caries Imaging Workstation' }}
          </div>
        </div>
      </div>

      <nav class="shell-navlist">
        <button
          v-for="item in navItems"
          :key="item.id"
          class="shell-navitem"
          :class="{ on: activeId === item.id }"
          @click="go(item.path)"
        >
          <AppIcon :name="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="shell-nav-foot">
        <div class="shell-model">
          <div class="shell-model-row">
            <span class="micro">Model</span>
            <span class="chip" style="height: 20px; font-size: 11px">v1.0 · Online</span>
          </div>
          <div class="shell-model-bar"><div style="width: 72%"></div></div>
          <div class="shell-model-row">
            <span class="mono" style="font-size: 11px; color: var(--ink-3)">Java BFF · Python AI</span>
          </div>
        </div>

        <div class="shell-user">
          <div class="shell-avatar">{{ avatarText }}</div>
          <div class="shell-user-meta">
            <div class="shell-user-name">{{ displayName }}</div>
            <div class="shell-user-id mono">{{ displayId }} · {{ t('common.today') }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="shell-search">
          <AppIcon name="search" :size="16" />
          <input :placeholder="t('common.search')" />
          <kbd>Ctrl K</kbd>
        </div>
        <div class="shell-top-actions">
          <button class="shell-top-btn" @click="toggleLang">
            <AppIcon name="globe" :size="16" />
            <span class="mono" style="font-size: 12px">{{ locale === 'zh' ? '中文 / EN' : 'EN / 中文' }}</span>
          </button>
          <button class="shell-top-btn">
            <AppIcon name="bell" :size="16" />
            <span class="shell-top-dot"></span>
          </button>
          <NButton type="primary" size="small" @click="go('/analysis')">
            <template #icon><AppIcon name="plus" :size="14" /></template>
            {{ t('common.newCase') }}
          </NButton>
        </div>
      </header>
      <main class="shell-view">
        <RouterView />
      </main>
    </div>
  </div>
</template>
