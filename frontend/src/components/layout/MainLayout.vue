<template>
  <AppLayout :active="activeKey" @navigate="navigate">
    <RouterView />
  </AppLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '@/views/AppLayout.vue'

const route = useRoute()
const router = useRouter()

const routeMap = {
  dashboard: '/dashboard',
  ai: '/ai-diagnosis',
  cases: '/cases',
  reports: '/reports',
  user: '/user-center',
  settings: '/settings',
} as const

const activeKey = computed(() => {
  if (route.path.startsWith(routeMap.ai) || route.path.startsWith('/analysis') || route.path.startsWith('/review')) return 'ai'
  if (route.path.startsWith(routeMap.cases)) return 'cases'
  if (route.path.startsWith(routeMap.reports)) return 'reports'
  if (route.path.startsWith(routeMap.user)) return 'user'
  if (route.path.startsWith(routeMap.settings)) return 'settings'
  return 'dashboard'
})

const navigate = (key: string) => {
  const target = routeMap[key as keyof typeof routeMap]
  if (target && target !== route.path) {
    router.push(target)
  }
}
</script>
