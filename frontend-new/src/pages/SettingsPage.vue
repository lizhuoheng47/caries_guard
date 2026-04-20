<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { locale } = useI18n()
</script>

<template>
  <div class="page">
    <h1 style="font-size: 24px; font-weight: 600; margin: 0 0 8px">{{ locale === 'zh' ? '系统设置' : 'Settings' }}</h1>
    <div class="card" style="padding: 24px">
      <div class="micro" style="margin-bottom: 12px">{{ locale === 'zh' ? '占位页面' : 'Placeholder' }}</div>
      <div style="color: var(--ink-3); font-size: 13px">
        {{ locale === 'zh' ? '此处包含模型版本管理、阈值调节、DICOM 接入、用户权限等。' : 'Model management, thresholds, DICOM integration, permissions.' }}
      </div>
    </div>
  </div>
</template>
