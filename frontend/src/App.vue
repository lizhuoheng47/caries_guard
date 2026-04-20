<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <router-view />
    <NotificationCenter />
  </n-config-provider>
</template>

<script setup lang="ts">
import { NConfigProvider } from 'naive-ui'
import NotificationCenter from '@/components/shared/NotificationCenter.vue'
import { themeOverrides } from '@/theme'
</script>
