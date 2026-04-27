<template>
  <div class="dent-shell">
    <!-- Background atmosphere -->
    <div class="bg-space"></div>
    <div class="bg-grid"></div>
    <div class="bg-glow bg-glow-a"></div>
    <div class="bg-glow bg-glow-b"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 36 42" fill="none" aria-hidden="true">
            <path
              d="M18 3L31 8.5V21.4C31 30.3 25.4 38 18 40.5C10.6 38 5 30.3 5 21.4V8.5L18 3Z"
              stroke="currentColor"
              stroke-width="2.4"
            />
            <path
              d="M18 14C15.5 11.7 11 13 10.4 17.5C9.9 21 11.6 23.4 12.7 26.4C13.7 29.2 13.8 33.7 16.2 34.4C18 35.1 17.9 31 18 28.4C18.1 31 18 35.1 19.8 34.4C22.2 33.7 22.3 29.2 23.3 26.4C24.4 23.4 26.1 21 25.6 17.5C25 13 20.5 11.7 18 14Z"
              fill="currentColor"
            />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">DentAI</div>
          <div class="brand-sub">智能影像辅助平台</div>
        </div>
      </div>

      <nav class="menu" role="navigation">
        <a
          v-for="item in menu"
          :key="item.key"
          class="menu-item"
          :class="{ active: item.key === active }"
          href="javascript:void(0)"
          @click="$emit('navigate', item.key)"
        >
          <span class="menu-icon" v-html="item.icon"></span>
          <span class="menu-label">{{ item.label }}</span>
          <span class="menu-indicator"></span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <slot name="footer">
          <div class="footer-card">
            <div class="footer-avatar">
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="9" r="3.5" stroke="currentColor" stroke-width="1.6" />
                <path d="M5 20c1.5-3.6 4-5 7-5s5.5 1.4 7 5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </div>
            <div class="footer-meta">
              <div class="footer-title">口腔影像科</div>
            </div>
            <svg class="footer-chev" viewBox="0 0 16 16" fill="none">
              <path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
        </slot>
      </div>
    </aside>

    <!-- Main page area -->
    <section class="main">
      <slot></slot>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{ active: string }>()
defineEmits<{ (e: 'navigate', key: string): void }>()

const menu = [
  {
    key: 'dashboard',
    label: '工作台',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M4 11l8-7 8 7v8.5a1.5 1.5 0 0 1-1.5 1.5H14v-6h-4v6H5.5A1.5 1.5 0 0 1 4 19.5V11Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
  },
  {
    key: 'ai',
    label: 'AI诊断分析',
    icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="6.5" cy="7" r="2" stroke="currentColor" stroke-width="1.6"/><circle cx="17.5" cy="7" r="2" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="17" r="2" stroke="currentColor" stroke-width="1.6"/><path d="M8.2 8.2l2.6 7M15.8 8.2l-2.6 7M8.5 7h7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'cases',
    label: '病例中心',
    icon: `<svg viewBox="0 0 24 24" fill="none"><rect x="5" y="4" width="14" height="17" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M9 9h6M9 13h6M9 17h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'reports',
    label: '数据报表',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M4 20h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><rect x="6" y="11" width="3" height="7" stroke="currentColor" stroke-width="1.6"/><rect x="11" y="7" width="3" height="11" stroke="currentColor" stroke-width="1.6"/><rect x="16" y="13" width="3" height="5" stroke="currentColor" stroke-width="1.6"/></svg>`,
  },
  {
    key: 'user',
    label: '用户中心',
    icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="9" r="3.5" stroke="currentColor" stroke-width="1.6"/><path d="M5 20c1.5-3.6 4-5 7-5s5.5 1.4 7 5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
  },
  {
    key: 'settings',
    label: '系统设置',
    icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.6"/><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
  },
]
</script>

<style scoped>
.dent-shell {
  --bg-deep: #02141a;
  --bg-mid: #051d24;
  --panel: rgba(11, 36, 44, 0.72);
  --panel-edge: rgba(94, 234, 212, 0.18);
  --text: #e8fff8;
  --text-soft: #9bc6bd;
  --text-dim: #5e8a82;
  --accent: #2ee6c8;
  --accent-2: #5eead4;
  --accent-warm: rgba(46, 230, 200, 0.12);
  position: relative;
  display: flex;
  width: 100%;
  min-height: 100vh;
  height: 100vh;
  color: var(--text);
  background: var(--bg-deep);
  font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans SC", system-ui, sans-serif;
  overflow: hidden;
}

.bg-space {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse at 8% 0%, rgba(46, 230, 200, 0.12), transparent 42%),
    radial-gradient(ellipse at 100% 100%, rgba(94, 234, 212, 0.07), transparent 50%),
    linear-gradient(180deg, #03161c 0%, #020c12 60%, #010a10 100%);
  z-index: 0;
}

.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(94, 234, 212, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(94, 234, 212, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse at 50% 50%, #000 0%, transparent 88%);
  z-index: 0;
}

.bg-glow {
  position: fixed;
  width: 700px;
  height: 700px;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.35;
  pointer-events: none;
  z-index: 0;
}

.bg-glow-a {
  top: -200px;
  left: 200px;
  background: rgba(46, 230, 200, 0.22);
}

.bg-glow-b {
  bottom: -250px;
  right: -120px;
  background: rgba(120, 86, 255, 0.18);
}

/* Sidebar */
.sidebar {
  position: relative;
  z-index: 2;
  width: 224px;
  flex-shrink: 0;
  padding: 24px 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  background: linear-gradient(180deg, rgba(7, 28, 35, 0.85), rgba(3, 14, 18, 0.95));
  border-right: 1px solid rgba(94, 234, 212, 0.08);
  backdrop-filter: blur(14px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 6px 18px;
  border-bottom: 1px solid rgba(94, 234, 212, 0.07);
}

.brand-mark {
  width: 42px;
  height: 48px;
  display: grid;
  place-items: center;
  color: var(--accent);
  filter: drop-shadow(0 0 10px rgba(46, 230, 200, 0.55));
}

.brand-mark svg {
  width: 36px;
  height: 42px;
}

.brand-name {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #f5fffb;
  line-height: 1.1;
}

.brand-sub {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-dim);
  letter-spacing: 1px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.menu-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  color: var(--text-soft);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}

.menu-item:hover {
  color: var(--text);
  background: rgba(46, 230, 200, 0.05);
}

.menu-item.active {
  color: var(--accent);
  background: linear-gradient(90deg, rgba(46, 230, 200, 0.18), rgba(46, 230, 200, 0.02));
  box-shadow: inset 0 0 24px rgba(46, 230, 200, 0.06);
}

.menu-item.active .menu-indicator {
  opacity: 1;
}

.menu-icon {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  color: currentColor;
}

.menu-icon svg,
.menu-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.menu-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
  box-shadow: 0 0 12px var(--accent);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.sidebar-footer {
  margin-top: auto;
}

.footer-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(94, 234, 212, 0.12);
  background: rgba(11, 36, 44, 0.65);
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.footer-card:hover {
  border-color: rgba(94, 234, 212, 0.28);
}

.footer-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(46, 230, 200, 0.1);
  color: var(--accent);
}

.footer-avatar svg {
  width: 18px;
  height: 18px;
}

.footer-meta {
  flex: 1;
  min-width: 0;
}

.footer-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.footer-chev {
  width: 14px;
  height: 14px;
  color: var(--text-dim);
}

/* Main */
.main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 28px 32px 32px;
  overflow-y: auto;
}
</style>
