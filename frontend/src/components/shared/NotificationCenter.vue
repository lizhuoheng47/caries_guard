<template>
  <div class="pointer-events-none fixed top-4 right-4 z-[120] flex w-[min(92vw,380px)] flex-col gap-3">
    <transition-group name="notification-stack" tag="div">
      <div
        v-for="item in notificationStore.items"
        :key="item.id"
        class="pointer-events-auto overflow-hidden rounded-md border backdrop-blur-xl shadow-[0_18px_40px_rgba(0,0,0,0.38)]"
        :class="toneClass[item.type]"
      >
        <div class="flex items-start gap-3 px-4 py-3.5">
          <div class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-[11px] font-bold" :class="badgeClass[item.type]">
            {{ iconMap[item.type] }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="font-mono text-[11px] uppercase tracking-[0.18em] text-[var(--td)]">
              {{ labelMap[item.type] }}
            </div>
            <div class="mt-1 text-[13px] font-medium text-[var(--tp)]">
              {{ item.title }}
            </div>
            <div v-if="item.message" class="mt-1 text-[12px] leading-relaxed text-[var(--ts)]">
              {{ item.message }}
            </div>
          </div>
          <button
            type="button"
            class="rounded border border-[var(--ln)] px-2 py-1 text-[10px] text-[var(--td)] transition-colors hover:text-[var(--tp)]"
            @click="notificationStore.remove(item.id)"
          >
            Close
          </button>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { useNotificationStore } from '@/stores/notification';
import type { NotificationType } from '@/stores/notification';

const notificationStore = useNotificationStore();

const iconMap: Record<NotificationType, string> = {
  success: 'OK',
  error: 'ER',
  info: 'IN',
  warning: 'WR',
};

const labelMap: Record<NotificationType, string> = {
  success: 'Success',
  error: 'Alert',
  info: 'Notice',
  warning: 'Warning',
};

const toneClass: Record<NotificationType, string> = {
  success: 'border-[rgba(16,185,129,0.35)] bg-[rgba(5,18,22,0.88)]',
  error: 'border-[rgba(244,63,94,0.35)] bg-[rgba(24,10,16,0.9)]',
  info: 'border-[rgba(0,229,255,0.35)] bg-[rgba(4,16,28,0.9)]',
  warning: 'border-[rgba(245,158,11,0.35)] bg-[rgba(26,16,6,0.9)]',
};

const badgeClass: Record<NotificationType, string> = {
  success: 'border-[rgba(16,185,129,0.45)] bg-[rgba(16,185,129,0.12)] text-[var(--emerald)]',
  error: 'border-[rgba(244,63,94,0.45)] bg-[rgba(244,63,94,0.12)] text-[var(--rose)]',
  info: 'border-[rgba(0,229,255,0.45)] bg-[rgba(0,229,255,0.12)] text-[var(--cyan)]',
  warning: 'border-[rgba(245,158,11,0.45)] bg-[rgba(245,158,11,0.12)] text-[var(--amber)]',
};
</script>

<style scoped>
.notification-stack-enter-active,
.notification-stack-leave-active {
  transition: all 0.25s ease;
}

.notification-stack-enter-from,
.notification-stack-leave-to {
  opacity: 0;
  transform: translateY(-10px) translateX(14px);
}

.notification-stack-move {
  transition: transform 0.25s ease;
}
</style>
