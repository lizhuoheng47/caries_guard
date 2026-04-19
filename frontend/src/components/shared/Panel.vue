<template>
  <div class="glass-panel rounded-md flex flex-col h-full overflow-hidden">
    <div v-if="title" class="panel-header flex items-center justify-between px-2 py-2 border-b-[0.5px] border-line-subtle bg-deep-surface/50">
      <div class="flex items-center gap-2">
        <div class="w-[3px] h-[11px]" :style="{ backgroundColor: `var(--${color})` }"></div>
        <h3 class="text-[11px] font-medium text-ice-white m-0 leading-none">{{ title }}</h3>
      </div>
      <div v-if="$slots.meta" class="panel-meta">
        <slot name="meta" />
      </div>
    </div>
    <div :class="['panel-content flex-1 overflow-auto', { 'p-3': !noPadding }]">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string;
  color?: 'cyan' | 'amber' | 'magenta' | 'emerald' | 'violet';
  noPadding?: boolean;
}>(), {
  color: 'cyan'
});
</script>
