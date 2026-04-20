<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NBadge, NAvatar, NInput, NButton } from 'naive-ui'
import AppIcon from '@/components/AppIcon.vue'
import { useI18n } from 'vue-i18n'
import { setLang } from '@/i18n'

const router = useRouter()
const { t, locale } = useI18n()

const navItems = computed(() => [
  { id: 'home', icon: 'home', label: t('nav.home') },
  { id: 'analyze', icon: 'scan', label: t('nav.analyze'), badge: 3 },
  { id: 'report', icon: 'report', label: t('nav.reports') },
  { id: 'library', icon: 'library', label: t('nav.library') },
  { id: 'settings', icon: 'settings', label: t('nav.settings') }
])

const go = (id: string) => router.push(`/app/${id}`)
const activeId = computed(() => router.currentRoute.value.name?.toString() || 'home')

const toggleLang = () => setLang(locale.value === 'zh' ? 'en' : 'zh')
</script>

<template>
  <div class="shell shell-classic">
    <aside class="shell-nav">
      <div class="shell-brand">
        <div class="shell-brand-mark"><AppIcon name="logo" :size="18" /></div>
        <div class="shell-brand-text">
          <div class="shell-brand-name">DentAI</div>
          <div class="shell-brand-role">{{ locale === 'zh' ? '北京口腔医院 · 修复科' : 'Stomatology · Restorative' }}</div>
        </div>
      </div>

      <nav class="shell-navlist">
        <button
          v-for="item in navItems"
          :key="item.id"
          class="shell-navitem"
          :class="{ on: activeId === item.id }"
          @click="go(item.id)"
        >
          <AppIcon :name="item.icon" :size="18" />
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="shell-navbadge">{{ item.badge }}</span>
        </button>
      </nav>

      <div class="shell-nav-foot">
        <div class="shell-model">
          <div class="shell-model-row">
            <span class="micro">Model</span>
            <span class="chip" style="height: 20px; font-size: 11px">v2.4 · Online</span>
          </div>
          <div class="shell-model-bar"><div style="width: 72%"></div></div>
          <div class="shell-model-row">
            <span class="mono" style="font-size: 11px; color: var(--ink-3)">GPU 72% · 0.8s avg</span>
          </div>
        </div>

        <div class="shell-user">
          <div class="shell-avatar">陈</div>
          <div class="shell-user-meta">
            <div class="shell-user-name">陈明 Dr. Chen</div>
            <div class="shell-user-id mono">DR-2041 · {{ t('common.today') }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="shell-search">
          <AppIcon name="search" :size="16" />
          <input :placeholder="t('common.search')" />
          <kbd>⌘K</kbd>
        </div>
        <div class="shell-top-actions">
          <button class="shell-top-btn" @click="toggleLang">
            <AppIcon name="globe" :size="16" />
            <span class="mono" style="font-size: 12px">{{ locale === 'zh' ? '中 / EN' : 'EN / 中' }}</span>
          </button>
          <button class="shell-top-btn">
            <AppIcon name="bell" :size="16" />
            <span class="shell-top-dot"></span>
          </button>
          <NButton type="primary" size="small" @click="go('analyze')">
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
