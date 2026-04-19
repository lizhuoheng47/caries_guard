<template>
  <button 
    class="font-mono text-[9px] font-medium uppercase tracking-[0.08em] px-[12px] py-[6px] rounded-[3px] flex items-center justify-center gap-[4px] transition-all relative overflow-hidden"
    :class="[variantClasses, disabled ? 'opacity-50 cursor-not-allowed' : '']"
    :disabled="disabled"
  >
    <slot name="icon-left"></slot>
    <span class="relative z-10"><slot></slot></span>
    <slot name="icon-right"></slot>
    <div v-if="variant === 'primary'" class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full hover:translate-x-full transition-transform duration-700"></div>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  variant?: 'primary' | 'ghost' | 'danger' | 'success';
  disabled?: boolean;
}>();

const variantClasses = computed(() => {
  switch (props.variant) {
    case 'primary':
      return 'bg-gradient-to-r from-[var(--cyan)]/20 to-[var(--violet)]/20 border border-[var(--cyan)] text-[var(--tp)] shadow-[0_0_14px_rgba(0,229,255,0.25)] hover:shadow-[0_0_20px_rgba(0,229,255,0.4)]';
    case 'danger':
      return 'bg-[var(--magenta)]/10 border border-[var(--magenta)]/35 text-[var(--magenta)] hover:bg-[var(--magenta)]/20';
    case 'success':
      return 'bg-gradient-to-r from-[var(--emerald)]/25 to-[var(--cyan)]/25 border border-[var(--emerald)] text-[var(--tp)] shadow-[0_0_10px_rgba(0,255,163,0.2)] hover:shadow-[0_0_15px_rgba(0,255,163,0.3)]';
    case 'ghost':
    default:
      return 'bg-[var(--cyan)]/5 border border-[var(--cyan)]/25 text-[var(--cyan-soft)] hover:bg-[var(--cyan)]/15';
  }
});
</script>
