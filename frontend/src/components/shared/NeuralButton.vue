<template>
  <button
    :class="[
      'neural-btn',
      `btn-${variant}`,
      { 'w-full': block }
    ]"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <slot name="icon-left" />
    <span class="btn-text"><slot /></span>
    <slot name="icon-right" />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  variant?: 'primary' | 'ghost' | 'danger' | 'success';
  block?: boolean;
  disabled?: boolean;
}>();

defineEmits(['click']);
</script>

<style scoped>
.neural-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 0.125rem;
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.neural-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Primary */
.btn-primary {
  background: linear-gradient(135deg, rgba(0,229,255,0.2), rgba(139,92,246,0.2));
  border: 0.5px solid var(--cyan);
  color: var(--tp);
  box-shadow: 0 0 14px rgba(0,229,255,0.25);
}
.btn-primary:not(:disabled):hover {
  background: linear-gradient(135deg, rgba(0,229,255,0.3), rgba(139,92,246,0.3));
  box-shadow: 0 0 20px rgba(0,229,255,0.4);
}

/* Ghost */
.btn-ghost {
  background: rgba(0,229,255,0.05);
  border: 0.5px solid rgba(0,229,255,0.25);
  color: var(--cyan-soft);
}
.btn-ghost:not(:disabled):hover {
  background: rgba(0,229,255,0.1);
  color: var(--cyan);
}

/* Danger */
.btn-danger {
  background: rgba(255,61,127,0.1);
  border: 0.5px solid rgba(255,61,127,0.35);
  color: var(--magenta);
}
.btn-danger:not(:disabled):hover {
  background: rgba(255,61,127,0.2);
  box-shadow: 0 0 14px rgba(255,61,127,0.25);
}

/* Success */
.btn-success {
  background: linear-gradient(135deg, rgba(0,255,163,0.25), rgba(0,229,255,0.25));
  border: 0.5px solid var(--emerald);
  color: var(--tp);
}
.btn-success:not(:disabled):hover {
  box-shadow: 0 0 14px rgba(0,255,163,0.25);
}
</style>
