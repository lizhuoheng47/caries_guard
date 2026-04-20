<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { setLang } from '@/i18n'
import AppIcon from '@/components/AppIcon.vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const router = useRouter()
const { t, locale } = useI18n()
let raf = 0

const onSubmit = (e: Event) => {
  e.preventDefault()
  router.push('/app/home')
}

onMounted(() => {
  const canvas = canvasRef.value!
  const ctx = canvas.getContext('2d')!
  let W = (canvas.width = canvas.offsetWidth * devicePixelRatio)
  let H = (canvas.height = canvas.offsetHeight * devicePixelRatio)
  const mouse = { x: -9999, y: -9999 }

  const onResize = () => {
    W = canvas.width = canvas.offsetWidth * devicePixelRatio
    H = canvas.height = canvas.offsetHeight * devicePixelRatio
  }
  const onMove = (e: MouseEvent) => {
    const r = canvas.getBoundingClientRect()
    mouse.x = (e.clientX - r.left) * devicePixelRatio
    mouse.y = (e.clientY - r.top) * devicePixelRatio
  }
  const onLeave = () => { mouse.x = -9999; mouse.y = -9999 }
  window.addEventListener('resize', onResize)
  canvas.addEventListener('mousemove', onMove)
  canvas.addEventListener('mouseleave', onLeave)

  const N = 90
  const parts = Array.from({ length: N }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    vx: (Math.random() - 0.5) * 0.25 * devicePixelRatio,
    vy: (Math.random() - 0.5) * 0.25 * devicePixelRatio,
    r: (Math.random() * 1.6 + 0.6) * devicePixelRatio
  }))

  const linkDist = 150 * devicePixelRatio
  const mouseRadius = 180 * devicePixelRatio

  const tick = () => {
    ctx.clearRect(0, 0, W, H)
    for (let i = 0; i < N; i++) {
      const a = parts[i]
      const dxm = mouse.x - a.x
      const dym = mouse.y - a.y
      const dm = Math.hypot(dxm, dym)
      if (dm < mouseRadius) {
        a.vx += (dxm / dm) * 0.05 * devicePixelRatio
        a.vy += (dym / dm) * 0.05 * devicePixelRatio
      }
      a.vx *= 0.985; a.vy *= 0.985
      a.x += a.vx; a.y += a.vy
      if (a.x < 0) a.x = W; if (a.x > W) a.x = 0
      if (a.y < 0) a.y = H; if (a.y > H) a.y = 0

      for (let j = i + 1; j < N; j++) {
        const b = parts[j]
        const dx = a.x - b.x, dy = a.y - b.y
        const d = Math.hypot(dx, dy)
        if (d < linkDist) {
          const alpha = (1 - d / linkDist) * 0.35
          ctx.strokeStyle = `rgba(18,165,148,${alpha})`
          ctx.lineWidth = 1 * devicePixelRatio
          ctx.beginPath()
          ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke()
        }
      }
      if (dm < mouseRadius) {
        const alpha = (1 - dm / mouseRadius) * 0.6
        ctx.strokeStyle = `rgba(62,193,176,${alpha})`
        ctx.lineWidth = 1 * devicePixelRatio
        ctx.beginPath()
        ctx.moveTo(a.x, a.y); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
      }
      const glow = dm < mouseRadius ? 1 - dm / mouseRadius : 0
      ctx.fillStyle = glow > 0.2 ? `rgba(125,217,204,${0.6 + glow * 0.4})` : 'rgba(18,165,148,.55)'
      ctx.beginPath(); ctx.arc(a.x, a.y, a.r * (1 + glow * 0.8), 0, Math.PI * 2); ctx.fill()
    }
    raf = requestAnimationFrame(tick)
  }
  tick()
})

onBeforeUnmount(() => cancelAnimationFrame(raf))
</script>

<template>
  <div class="login-root">
    <canvas ref="canvasRef" class="login-canvas"></canvas>
    <div class="login-grid-overlay" aria-hidden="true"></div>
    <div class="login-content">
      <div class="login-brand">
        <div class="login-logo"><AppIcon name="logo" :size="28" /></div>
        <div>
          <div class="login-brand-name">DentAI · 智齿</div>
          <div class="login-brand-sub">Caries Diagnostic Assistant · v2.4</div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-hero">
          <div class="login-eyebrow">
            <span class="login-dot"></span>
            <span>{{ t('login.eyebrow') }}</span>
          </div>
          <h1 class="login-title" style="white-space: pre-line">{{ t('login.title') }}</h1>
          <p class="login-sub">{{ t('login.sub') }}</p>
        </div>

        <form class="login-form" @submit="onSubmit">
          <label class="login-field">
            <span>{{ t('login.account') }}</span>
            <input type="text" value="DR-2041" autocomplete="username" />
          </label>
          <label class="login-field">
            <span>{{ t('login.password') }}</span>
            <input type="password" value="••••••••••" autocomplete="current-password" />
          </label>
          <div class="login-row">
            <label class="login-check">
              <input type="checkbox" checked />
              <span>{{ t('login.remember') }}</span>
            </label>
            <a href="#" class="login-link">{{ t('login.forgot') }}</a>
          </div>
          <button type="submit" class="btn btn-primary btn-lg login-submit">
            {{ t('login.signIn') }}
            <AppIcon name="arrow_right" :size="16" />
          </button>
          <button type="button" class="btn btn-ghost btn-lg login-sso">
            <AppIcon name="user" :size="14" />
            {{ t('login.sso') }}
          </button>
        </form>

        <div class="login-foot">
          <AppIcon name="alert" :size="13" />
          <span>{{ t('login.disclaimer') }}</span>
        </div>
      </div>

      <div class="login-lang">
        <button :class="{ on: locale === 'zh' }" @click="setLang('zh')">中文</button>
        <span class="sep">·</span>
        <button :class="{ on: locale === 'en' }" @click="setLang('en')">EN</button>
      </div>

      <div class="login-marks">
        <div class="lm-row">
          <span class="mono">ISO&nbsp;13485</span>
          <span class="mono">NMPA&nbsp;II</span>
          <span class="mono">HIPAA</span>
          <span class="mono">SOC&nbsp;2</span>
        </div>
        <div class="lm-stat">
          <div><div class="lm-n">98.2<span>%</span></div><div class="lm-l">{{ locale === 'zh' ? '病灶检出率' : 'Detection' }}</div></div>
          <div><div class="lm-n">0.8<span>s</span></div><div class="lm-l">{{ locale === 'zh' ? '平均分析' : 'Avg. analysis' }}</div></div>
          <div><div class="lm-n">1.4<span>M</span></div><div class="lm-l">{{ locale === 'zh' ? '训练样本' : 'Samples' }}</div></div>
        </div>
      </div>
    </div>
  </div>
</template>
