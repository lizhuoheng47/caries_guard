<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { setLang } from '@/i18n'
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { ApiClientError } from '@/api/request'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { t, locale } = useI18n()

const username = ref('admin')
const password = ref('123456')
const keepSignedIn = ref(true)
const loading = ref(false)
const raf = ref(0)
const sessionId = ref('')

let onResize: (() => void) | null = null
let onMove: ((event: MouseEvent) => void) | null = null
let onLeave: (() => void) | null = null

const resolveLoginError = (error: unknown) => {
  const normalized =
    error instanceof ApiClientError
      ? error
      : new ApiClientError(error instanceof Error ? error.message : 'Login failed', { cause: error })

  if (normalized.code === 'A0003') return { title: '账号或密码错误', message: '请输入正确的账号和密码后重试。' }
  if (normalized.code === 'A0004') return { title: '账号已被禁用', message: '当前账号不可登录，请联系管理员处理。' }
  if (normalized.code === 'B0001') return { title: '登录信息不完整', message: '用户名和密码不能为空。' }
  if (normalized.isNetworkError || normalized.code === 'NETWORK_ERROR') {
    return { title: '网络错误', message: '无法连接登录服务，请确认后端已启动。' }
  }
  if (normalized.isTimeout) return { title: '登录超时', message: '服务响应超时，请稍后重试。' }

  const traceSuffix = normalized.traceId ? ` 追踪 ID: ${normalized.traceId}` : ''
  return {
    title: '登录失败',
    message: `${normalized.message || '登录请求未完成。'}${traceSuffix}`
  }
}

const handleLogin = async () => {
  if (loading.value) return

  if (!username.value.trim() || !password.value.trim()) {
    notificationStore.warning('登录信息不完整', '用户名和密码不能为空。')
    return
  }

  loading.value = true
  try {
    await authStore.login({ username: username.value, password: password.value })
    notificationStore.success('登录成功', '正在进入系统。')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard/ai'
    await router.push(redirect)
  } catch (error) {
    const feedback = resolveLoginError(error)
    notificationStore.error(feedback.title, feedback.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  sessionId.value = `NC-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.random()
    .toString(16)
    .slice(2, 6)
    .toUpperCase()}`

  const canvas = canvasRef.value
  if (!canvas) return

  const context = canvas.getContext('2d')
  if (!context) return

  const mouse = { x: -9999, y: -9999 }
  let width = canvas.offsetWidth * devicePixelRatio
  let height = canvas.offsetHeight * devicePixelRatio
  canvas.width = width
  canvas.height = height

  onResize = () => {
    width = canvas.offsetWidth * devicePixelRatio
    height = canvas.offsetHeight * devicePixelRatio
    canvas.width = width
    canvas.height = height
  }

  onMove = (event: MouseEvent) => {
    const rect = canvas.getBoundingClientRect()
    mouse.x = (event.clientX - rect.left) * devicePixelRatio
    mouse.y = (event.clientY - rect.top) * devicePixelRatio
  }

  onLeave = () => {
    mouse.x = -9999
    mouse.y = -9999
  }

  window.addEventListener('resize', onResize)
  canvas.addEventListener('mousemove', onMove)
  canvas.addEventListener('mouseleave', onLeave)

  const particles = Array.from({ length: 90 }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.25 * devicePixelRatio,
    vy: (Math.random() - 0.5) * 0.25 * devicePixelRatio,
    r: (Math.random() * 1.6 + 0.6) * devicePixelRatio
  }))

  const linkDistance = 150 * devicePixelRatio
  const mouseRadius = 180 * devicePixelRatio

  const tick = () => {
    context.clearRect(0, 0, width, height)

    for (let i = 0; i < particles.length; i += 1) {
      const a = particles[i]
      const dxMouse = mouse.x - a.x
      const dyMouse = mouse.y - a.y
      const distMouse = Math.hypot(dxMouse, dyMouse)

      if (distMouse > 0 && distMouse < mouseRadius) {
        a.vx += (dxMouse / distMouse) * 0.05 * devicePixelRatio
        a.vy += (dyMouse / distMouse) * 0.05 * devicePixelRatio
      }

      a.vx *= 0.985
      a.vy *= 0.985
      a.x += a.vx
      a.y += a.vy

      if (a.x < 0) a.x = width
      if (a.x > width) a.x = 0
      if (a.y < 0) a.y = height
      if (a.y > height) a.y = 0

      for (let j = i + 1; j < particles.length; j += 1) {
        const b = particles[j]
        const distance = Math.hypot(a.x - b.x, a.y - b.y)
        if (distance < linkDistance) {
          const alpha = (1 - distance / linkDistance) * 0.35
          context.strokeStyle = `rgba(18,165,148,${alpha})`
          context.lineWidth = 1 * devicePixelRatio
          context.beginPath()
          context.moveTo(a.x, a.y)
          context.lineTo(b.x, b.y)
          context.stroke()
        }
      }

      if (distMouse < mouseRadius) {
        const alpha = (1 - distMouse / mouseRadius) * 0.6
        context.strokeStyle = `rgba(62,193,176,${alpha})`
        context.lineWidth = 1 * devicePixelRatio
        context.beginPath()
        context.moveTo(a.x, a.y)
        context.lineTo(mouse.x, mouse.y)
        context.stroke()
      }

      const glow = distMouse < mouseRadius ? 1 - distMouse / mouseRadius : 0
      context.fillStyle =
        glow > 0.2 ? `rgba(125,217,204,${0.6 + glow * 0.4})` : 'rgba(18,165,148,.55)'
      context.beginPath()
      context.arc(a.x, a.y, a.r * (1 + glow * 0.8), 0, Math.PI * 2)
      context.fill()
    }

    raf.value = requestAnimationFrame(tick)
  }

  tick()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf.value)
  if (onResize) window.removeEventListener('resize', onResize)
  if (canvasRef.value && onMove) canvasRef.value.removeEventListener('mousemove', onMove)
  if (canvasRef.value && onLeave) canvasRef.value.removeEventListener('mouseleave', onLeave)
})
</script>

<template>
  <div class="login-root">
    <canvas ref="canvasRef" class="login-canvas"></canvas>
    <div class="login-grid-overlay" aria-hidden="true"></div>
    <div class="login-content">
      <div class="login-brand">
        <div class="login-logo"><AppIcon name="logo" :size="28" /></div>
        <div>
          <div class="login-brand-name">{{ t('product') }}</div>
          <div class="login-brand-sub">Caries Diagnostic Assistant · v1.0</div>
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

        <form class="login-form" @submit.prevent="handleLogin">
          <label class="login-field">
            <span>{{ t('login.account') }}</span>
            <input v-model.trim="username" type="text" autocomplete="username" />
          </label>
          <label class="login-field">
            <span>{{ t('login.password') }}</span>
            <input v-model="password" type="password" autocomplete="current-password" />
          </label>
          <div class="login-row">
            <label class="login-check">
              <input v-model="keepSignedIn" type="checkbox" />
              <span>{{ t('login.remember') }}</span>
            </label>
            <a href="#" class="login-link" @click.prevent>{{ t('login.forgot') }}</a>
          </div>
          <button type="submit" class="btn btn-primary btn-lg login-submit" :disabled="loading">
            {{ loading ? '登录中...' : t('login.signIn') }}
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
          <span class="mono">Session {{ sessionId }}</span>
        </div>
        <div class="lm-stat">
          <div>
            <div class="lm-n">98.2<span>%</span></div>
            <div class="lm-l">{{ locale === 'zh' ? '检测召回' : 'Detection' }}</div>
          </div>
          <div>
            <div class="lm-n">0.8<span>s</span></div>
            <div class="lm-l">{{ locale === 'zh' ? '平均分析' : 'Latency' }}</div>
          </div>
          <div>
            <div class="lm-n">24<span>/7</span></div>
            <div class="lm-l">{{ locale === 'zh' ? '在线运行' : 'Runtime' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
