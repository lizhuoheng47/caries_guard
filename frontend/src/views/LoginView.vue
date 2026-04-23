<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, Fingerprint, LockKeyhole, UserRound } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const username = ref('admin')
const password = ref('123456')
const showPassword = ref(false)
const loading = ref(false)

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
</script>

<template>
  <div class="login-shell">
    <div class="login-backdrop" aria-hidden="true">
      <div class="login-grid"></div>
      <div class="login-orb login-orb-left"></div>
      <div class="login-orb login-orb-right"></div>
      <div class="login-stars"></div>
    </div>

    <div class="login-layout">
      <section class="hero-panel">
        <div class="brand-pill">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M12 3.3L18.4 5.7V11.3C18.4 15.1 15.8 18.5 12 19.7C8.2 18.5 5.6 15.1 5.6 11.3V5.7L12 3.3Z"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linejoin="round"
              />
              <path
                d="M12 7.2V12.8"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
              />
              <path
                d="M9.3 10.1H14.7"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
              />
            </svg>
          </div>
          <div>
            <div class="brand-title">龋智卫 DentAI</div>
            <div class="brand-subtitle">Clinical Intelligence Platform</div>
          </div>
        </div>

        <div class="hero-copy">
          <div class="hero-kicker">DentAI Clinical Suite</div>
          <h1>
            龋齿<span>AI</span>智能
            <br />
            医疗辅助系统
          </h1>
          <p>AI分析 · 病灶检测 · 知识增强 · 医生复核</p>
        </div>

        <div class="hero-stage">
          <div class="tooth-halo"></div>

          <div class="feature-chip feature-analysis">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M4 14H8L10.2 8L13.8 16L16 10H20" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
            <span>AI分析</span>
          </div>

          <div class="feature-chip feature-detect">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M8 4H4V8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M16 4H20V8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M8 20H4V16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M16 20H20V16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <circle cx="12" cy="12" r="2.8" stroke="currentColor" stroke-width="1.8" />
              </svg>
            </div>
            <span>病灶检测</span>
          </div>

          <div class="feature-chip feature-report">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M6 5.5H11C12.1 5.5 13 6.4 13 7.5V18.5H8C6.9 18.5 6 17.6 6 16.5V5.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
                <path d="M18 5.5H13C11.9 5.5 11 6.4 11 7.5V18.5H16C17.1 18.5 18 17.6 18 16.5V5.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
              </svg>
            </div>
            <span>知识增强</span>
          </div>

          <div class="feature-chip feature-review">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="8" r="3.2" stroke="currentColor" stroke-width="1.8" />
                <path d="M5.5 18.5C6.7 15.7 9 14.3 12 14.3C15 14.3 17.3 15.7 18.5 18.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
            </div>
            <span>医生复核</span>
          </div>

          <svg class="tooth-visual" viewBox="0 0 520 520" aria-hidden="true">
            <defs>
              <radialGradient id="toothGlow" cx="50%" cy="50%" r="60%">
                <stop offset="0%" stop-color="rgba(82,244,255,0.95)" />
                <stop offset="55%" stop-color="rgba(54,166,255,0.45)" />
                <stop offset="100%" stop-color="rgba(9,18,48,0)" />
              </radialGradient>
              <linearGradient id="toothStroke" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#7CEEFF" />
                <stop offset="50%" stop-color="#3CE0FF" />
                <stop offset="100%" stop-color="#7A7BFF" />
              </linearGradient>
              <filter id="toothBlur">
                <feGaussianBlur stdDeviation="5" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            <ellipse cx="260" cy="432" rx="160" ry="30" fill="rgba(50,212,255,0.18)" />
            <ellipse cx="260" cy="432" rx="112" ry="16" fill="rgba(50,212,255,0.26)" />
            <circle cx="260" cy="262" r="158" fill="url(#toothGlow)" />

            <g filter="url(#toothBlur)">
              <path
                d="M176 98C136 103 106 132 98 176C92 208 99 244 112 281C123 311 139 342 153 367C163 384 170 403 183 415C193 425 208 428 220 418C232 407 239 389 244 372C250 348 257 318 282 318C307 318 314 348 320 372C325 389 332 407 344 418C356 428 371 425 381 415C394 403 401 384 411 367C425 342 441 311 452 281C465 244 472 208 466 176C458 132 428 103 388 98C359 95 333 107 310 126C303 132 296 139 289 147C282 139 275 132 268 126C245 107 219 95 190 98H176Z"
                fill="none"
                stroke="url(#toothStroke)"
                stroke-width="4"
              />
              <path
                d="M153 172C142 197 143 231 151 264C160 301 180 334 201 365"
                fill="none"
                stroke="url(#toothStroke)"
                stroke-opacity="0.9"
                stroke-width="2.4"
              />
              <path
                d="M411 172C422 197 421 231 413 264C404 301 384 334 363 365"
                fill="none"
                stroke="url(#toothStroke)"
                stroke-opacity="0.9"
                stroke-width="2.4"
              />
              <path
                d="M230 125C252 147 260 176 261 210"
                fill="none"
                stroke="url(#toothStroke)"
                stroke-opacity="0.72"
                stroke-width="2.2"
              />
              <path
                d="M332 125C310 147 302 176 301 210"
                fill="none"
                stroke="url(#toothStroke)"
                stroke-opacity="0.72"
                stroke-width="2.2"
              />
            </g>

            <g class="tooth-points">
              <circle cx="118" cy="178" r="5" />
              <circle cx="162" cy="100" r="5" />
              <circle cx="241" cy="126" r="5" />
              <circle cx="351" cy="100" r="5" />
              <circle cx="400" cy="182" r="5" />
              <circle cx="159" cy="365" r="5" />
              <circle cx="220" cy="414" r="5" />
              <circle cx="344" cy="414" r="5" />
              <circle cx="407" cy="365" r="5" />
            </g>
          </svg>
        </div>
      </section>

      <section class="auth-panel">
        <form class="login-card" @submit.prevent="handleLogin">
          <div class="card-edge" aria-hidden="true"></div>

          <div class="card-head">
            <div>
              <h2>登录平台</h2>
              <span class="card-head-bar"></span>
            </div>
            <div class="card-fingerprint">
              <Fingerprint :size="28" aria-hidden="true" />
            </div>
          </div>

          <label class="field">
            <span class="sr-only">用户名</span>
            <UserRound class="field-icon" :size="18" aria-hidden="true" />
            <input
              v-model.trim="username"
              type="text"
              autocomplete="username"
              placeholder="用户名 / admin"
              :disabled="loading"
            />
          </label>

          <label class="field">
            <span class="sr-only">密码</span>
            <LockKeyhole class="field-icon" :size="18" aria-hidden="true" />
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="密码"
              :disabled="loading"
            />
            <button
              type="button"
              class="field-toggle"
              :disabled="loading"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M3 3L21 21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M10.7 10.9C10.3 11.2 10 11.8 10 12.5C10 13.6 10.9 14.5 12 14.5C12.7 14.5 13.3 14.2 13.6 13.7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M8 6.8C9.2 6.2 10.5 5.9 12 5.9C16.1 5.9 19 8.1 21 12C20.1 13.8 19 15.2 17.6 16.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M14.7 18C13.8 18.3 12.9 18.4 12 18.4C7.9 18.4 5 16.2 3 12.3C3.8 10.7 4.8 9.4 6 8.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M2.8 12C4.8 8.1 7.7 5.9 12 5.9C16.3 5.9 19.2 8.1 21.2 12C19.2 15.9 16.3 18.1 12 18.1C7.7 18.1 4.8 15.9 2.8 12Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
                <circle cx="12" cy="12" r="3.1" stroke="currentColor" stroke-width="1.8" />
              </svg>
            </button>
          </label>

          <div class="card-actions">
            <button type="button" class="link-button">忘记密码？</button>
          </div>

          <button type="submit" class="submit-button" :disabled="loading">
            <span>{{ loading ? '登录中...' : '进入系统' }}</span>
            <ArrowRight :size="18" aria-hidden="true" />
          </button>

          <div class="safety-line">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M12 3.3L18.4 5.7V11.3C18.4 15.1 15.8 18.5 12 19.7C8.2 18.5 5.6 15.1 5.6 11.3V5.7L12 3.3Z"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linejoin="round"
              />
            </svg>
            <span>安全访问 · 数据加密 · 隐私保护</span>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(circle at 14% 18%, rgba(40, 174, 255, 0.18), transparent 26%),
    radial-gradient(circle at 86% 22%, rgba(57, 214, 255, 0.14), transparent 24%),
    radial-gradient(circle at 50% 100%, rgba(17, 90, 192, 0.22), transparent 40%),
    linear-gradient(180deg, #041122 0%, #061a35 52%, #041024 100%);
  color: #f6fbff;
}

.login-backdrop,
.login-grid,
.login-stars,
.login-orb {
  pointer-events: none;
  position: absolute;
  inset: 0;
}

.login-grid {
  background-image:
    linear-gradient(rgba(115, 164, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(115, 164, 255, 0.07) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.9), transparent 92%);
}

.login-stars {
  background-image:
    radial-gradient(circle at 10% 22%, rgba(255, 255, 255, 0.55) 0 1px, transparent 1.4px),
    radial-gradient(circle at 28% 36%, rgba(103, 206, 255, 0.55) 0 1px, transparent 1.4px),
    radial-gradient(circle at 52% 18%, rgba(255, 255, 255, 0.4) 0 1px, transparent 1.4px),
    radial-gradient(circle at 74% 30%, rgba(77, 196, 255, 0.4) 0 1px, transparent 1.4px),
    radial-gradient(circle at 84% 76%, rgba(255, 255, 255, 0.35) 0 1px, transparent 1.4px);
  opacity: 0.9;
}

.login-orb {
  filter: blur(36px);
  opacity: 0.65;
}

.login-orb-left {
  background: radial-gradient(circle, rgba(42, 214, 255, 0.28) 0%, transparent 62%);
  transform: translate(-14%, -20%);
}

.login-orb-right {
  background: radial-gradient(circle, rgba(100, 108, 255, 0.22) 0%, transparent 56%);
  transform: translate(24%, 2%);
}

.login-layout {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: clamp(24px, 4vw, 56px);
  min-height: 100dvh;
  max-width: 1480px;
  margin: 0 auto;
  padding: clamp(18px, 3vw, 42px) clamp(16px, 4vw, 48px);
}

.hero-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1 1 52rem;
  gap: clamp(18px, 3vh, 30px);
  min-width: 0;
  max-width: min(100%, 760px);
}

.brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  width: fit-content;
  max-width: min(100%, 360px);
  padding: clamp(10px, 1.2vw, 12px) clamp(14px, 1.5vw, 18px);
  border: 1px solid rgba(149, 205, 255, 0.18);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(18, 35, 69, 0.9), rgba(10, 25, 54, 0.7));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.03), 0 18px 42px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(18px);
}

.brand-icon {
  display: grid;
  place-items: center;
  width: clamp(40px, 3vw, 46px);
  height: clamp(40px, 3vw, 46px);
  border-radius: clamp(12px, 1vw, 14px);
  color: #55f2ff;
  background: linear-gradient(135deg, rgba(79, 247, 255, 0.24), rgba(58, 103, 255, 0.16));
  box-shadow: 0 0 22px rgba(73, 231, 255, 0.22);
}

.brand-icon svg {
  width: clamp(20px, 1.5vw, 24px);
  height: clamp(20px, 1.5vw, 24px);
}

.brand-title {
  font-size: clamp(16px, 1.2vw, 18px);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: clamp(9px, 0.7vw, 11px);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(214, 236, 255, 0.54);
}

.hero-copy {
  max-width: min(100%, 640px);
}

.hero-kicker {
  margin: 0 0 clamp(10px, 1.8vh, 16px);
  font-size: clamp(10px, 0.8vw, 12px);
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(154, 218, 255, 0.64);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(36px, 4.7vw, 74px);
  line-height: 1.04;
  letter-spacing: -0.04em;
  font-weight: 800;
}

.hero-copy h1 span {
  margin: 0 6px;
  color: #42eaff;
  text-shadow: 0 0 24px rgba(66, 234, 255, 0.32);
}

.hero-copy p {
  margin: clamp(12px, 2vh, 18px) 0 0;
  font-size: clamp(15px, 1.55vw, 22px);
  line-height: 1.55;
  color: rgba(224, 238, 255, 0.78);
}

.hero-stage {
  position: relative;
  width: min(100%, 720px);
  min-height: clamp(250px, 38vh, 420px);
}

.tooth-halo {
  position: absolute;
  left: 50%;
  top: 56%;
  width: clamp(260px, 31vw, 420px);
  height: clamp(260px, 31vw, 420px);
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(55, 228, 255, 0.18) 0%, rgba(55, 228, 255, 0.04) 50%, transparent 72%);
  filter: blur(18px);
  animation: pulse 4.8s ease-in-out infinite;
}

.tooth-visual {
  position: relative;
  z-index: 1;
  display: block;
  width: min(100%, clamp(320px, 41vw, 600px));
  margin: 0 auto;
  overflow: visible;
}

.tooth-points circle {
  fill: #dffaff;
  filter: drop-shadow(0 0 10px rgba(159, 242, 255, 0.8));
}

.feature-chip {
  position: absolute;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  min-width: clamp(118px, 10vw, 148px);
  padding: clamp(8px, 1vw, 12px) clamp(10px, 1.15vw, 14px);
  border: 1px solid rgba(126, 181, 255, 0.18);
  border-radius: clamp(14px, 1.5vw, 18px);
  background: linear-gradient(180deg, rgba(12, 27, 57, 0.88), rgba(8, 22, 50, 0.64));
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
  animation: floatY 5.4s ease-in-out infinite;
}

.feature-analysis {
  left: 1.5%;
  top: 24%;
}

.feature-detect {
  left: 3%;
  bottom: 8%;
  animation-delay: 0.8s;
}

.feature-report {
  right: 4%;
  top: 24%;
  animation-delay: 1.2s;
}

.feature-review {
  right: 1.5%;
  bottom: 6%;
  animation-delay: 1.8s;
}

.feature-icon {
  display: grid;
  place-items: center;
  width: clamp(34px, 3vw, 42px);
  height: clamp(34px, 3vw, 42px);
  border-radius: clamp(11px, 1vw, 14px);
  background: linear-gradient(135deg, rgba(74, 245, 255, 0.24), rgba(104, 117, 255, 0.16));
  color: #7ff5ff;
  flex-shrink: 0;
}

.feature-icon svg {
  width: clamp(16px, 1.45vw, 20px);
  height: clamp(16px, 1.45vw, 20px);
}

.feature-chip span {
  font-size: clamp(12px, 0.95vw, 15px);
  font-weight: 600;
  color: rgba(240, 248, 255, 0.88);
}

.auth-panel {
  display: flex;
  justify-content: center;
  flex: 0 1 clamp(340px, 31vw, 438px);
  width: min(100%, 438px);
  margin-left: auto;
}

.login-card {
  position: relative;
  width: 100%;
  padding: clamp(24px, 2.3vw, 34px) clamp(20px, 2.3vw, 34px) clamp(20px, 2vw, 26px);
  border: 1px solid rgba(158, 212, 255, 0.3);
  border-radius: clamp(22px, 2vw, 28px);
  background:
    linear-gradient(180deg, rgba(22, 40, 78, 0.86), rgba(8, 18, 42, 0.84)),
    radial-gradient(circle at top right, rgba(59, 235, 255, 0.18), transparent 35%);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    0 28px 72px rgba(1, 8, 24, 0.48);
  backdrop-filter: blur(20px);
}

.card-edge {
  position: absolute;
  inset: 0;
  border-radius: clamp(22px, 2vw, 28px);
  padding: 1px;
  background: linear-gradient(135deg, rgba(129, 219, 255, 0.45), rgba(62, 243, 255, 0.18), rgba(122, 121, 255, 0.42));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: clamp(14px, 1.8vw, 24px);
  margin-bottom: clamp(20px, 2.2vh, 28px);
}

.card-head h2 {
  margin: 0;
  font-size: clamp(28px, 2.7vw, 38px);
  line-height: 1.1;
  font-weight: 700;
}

.card-head-bar {
  display: inline-block;
  width: clamp(28px, 2vw, 34px);
  height: 3px;
  margin-top: clamp(10px, 1vh, 14px);
  border-radius: 999px;
  background: linear-gradient(90deg, #3fe8ff, #4da5ff);
  box-shadow: 0 0 16px rgba(63, 232, 255, 0.42);
}

.card-fingerprint {
  display: grid;
  place-items: center;
  width: clamp(50px, 4vw, 58px);
  height: clamp(50px, 4vw, 58px);
  border-radius: clamp(16px, 1.4vw, 18px);
  color: #64efff;
  background: linear-gradient(135deg, rgba(74, 245, 255, 0.12), rgba(113, 111, 255, 0.14));
  border: 1px solid rgba(158, 212, 255, 0.16);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.03);
}

.field {
  position: relative;
  display: block;
  margin-bottom: clamp(12px, 1.4vh, 16px);
}

.field input {
  width: 100%;
  height: clamp(58px, 7.4vh, 70px);
  padding: 0 clamp(44px, 4vw, 54px) 0 clamp(44px, 4vw, 52px);
  border: 1px solid rgba(146, 196, 255, 0.15);
  border-radius: clamp(16px, 1.5vw, 18px);
  background: linear-gradient(180deg, rgba(14, 31, 66, 0.78), rgba(12, 24, 54, 0.58));
  color: #f3fbff;
  font-size: clamp(15px, 1.25vw, 18px);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.field input::placeholder {
  color: rgba(208, 229, 255, 0.4);
}

.field input:focus-visible {
  outline: none;
  border-color: rgba(99, 226, 255, 0.55);
  box-shadow: 0 0 0 4px rgba(63, 232, 255, 0.12);
  transform: translateY(-1px);
}

.field input:disabled {
  cursor: not-allowed;
  opacity: 0.74;
}

.field-icon {
  position: absolute;
  left: clamp(16px, 1.4vw, 20px);
  top: 50%;
  transform: translateY(-50%);
  color: rgba(206, 229, 255, 0.44);
}

.field-toggle {
  position: absolute;
  right: clamp(12px, 1vw, 16px);
  top: 50%;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(206, 229, 255, 0.46);
  transform: translateY(-50%);
  cursor: pointer;
}

.field-toggle svg {
  width: 18px;
  height: 18px;
}

.field-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin: 4px 0 clamp(14px, 2vh, 18px);
}

.link-button {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: clamp(12px, 0.95vw, 14px);
  color: rgba(190, 216, 255, 0.76);
  cursor: pointer;
}

.link-button:hover {
  color: #ffffff;
}

.submit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: clamp(60px, 8vh, 72px);
  border: 0;
  border-radius: clamp(16px, 1.5vw, 18px);
  background: linear-gradient(90deg, #3cecff 0%, #43d6ff 44%, #6669ff 100%);
  color: #06213f;
  font-size: clamp(18px, 1.6vw, 22px);
  font-weight: 800;
  box-shadow: 0 22px 54px rgba(67, 180, 255, 0.34);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.04);
  box-shadow: 0 26px 58px rgba(67, 180, 255, 0.4);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.safety-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: clamp(18px, 2vh, 26px);
  color: rgba(190, 216, 255, 0.58);
  font-size: clamp(11px, 0.92vw, 13px);
}

.safety-line svg {
  width: 16px;
  height: 16px;
  color: rgba(112, 236, 255, 0.72);
}

@keyframes floatY {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(0.98);
    opacity: 0.68;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.04);
    opacity: 1;
  }
}

@media (max-width: 1200px) {
  .login-layout {
    justify-content: center;
    gap: 36px;
    padding: 32px 24px 40px;
  }

  .hero-panel {
    flex-basis: 100%;
    max-width: 760px;
    align-items: center;
    text-align: center;
  }

  .auth-panel {
    flex-basis: min(100%, 438px);
    margin-left: 0;
  }

  .hero-stage {
    width: min(100%, 680px);
  }

  .feature-analysis {
    left: 12px;
    top: 122px;
  }

  .feature-detect {
    left: 20px;
  }

  .feature-report {
    right: 20px;
    top: 122px;
  }

  .feature-review {
    right: 12px;
  }
}

@media (max-width: 840px) {
  .hero-stage {
    min-height: 360px;
  }

  .feature-chip {
    min-width: 124px;
    gap: 10px;
    padding: 10px 12px;
  }

  .feature-chip span {
    font-size: 13px;
  }

  .feature-icon {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .feature-analysis {
    left: 0;
    top: 92px;
  }

  .feature-detect {
    left: 8px;
    bottom: 44px;
  }

  .feature-report {
    right: 8px;
    top: 92px;
  }

  .feature-review {
    right: 0;
    bottom: 44px;
  }

  .login-card {
    padding: 28px 22px 22px;
  }

  .card-head h2 {
    font-size: 30px;
  }
}

@media (max-height: 860px) and (min-width: 841px) {
  .login-layout {
    padding-top: 18px;
    padding-bottom: 18px;
  }

  .hero-panel {
    gap: 16px;
  }

  .hero-copy h1 {
    font-size: clamp(34px, 4.1vw, 60px);
  }

  .hero-copy p {
    margin-top: 10px;
    font-size: clamp(14px, 1.2vw, 18px);
  }

  .hero-stage {
    min-height: clamp(220px, 32vh, 330px);
  }

  .tooth-halo {
    width: clamp(220px, 24vw, 320px);
    height: clamp(220px, 24vw, 320px);
  }

  .tooth-visual {
    width: min(100%, clamp(280px, 32vw, 500px));
  }

  .feature-chip {
    min-width: 110px;
    padding: 8px 10px;
  }

  .login-card {
    padding: 22px 22px 18px;
  }

  .field input {
    height: 58px;
  }

  .submit-button {
    height: 60px;
  }

  .safety-line {
    margin-top: 16px;
  }
}

@media (max-width: 640px) {
  .login-layout {
    padding: 20px 16px 32px;
    gap: 28px;
  }

  .brand-pill {
    width: 100%;
    max-width: 320px;
    justify-content: flex-start;
  }

  .hero-copy h1 {
    font-size: 40px;
  }

  .hero-copy p {
    font-size: 16px;
  }

  .hero-stage {
    min-height: 300px;
  }

  .feature-chip {
    position: static;
    min-width: 0;
    width: calc(50% - 8px);
    animation: none;
  }

  .hero-stage {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
  }

  .tooth-halo {
    width: 280px;
    height: 280px;
    top: 46%;
  }

  .tooth-visual {
    order: -1;
    width: 100%;
    max-width: 360px;
  }

  .auth-panel {
    width: 100%;
  }

  .card-head {
    margin-bottom: 24px;
  }

  .card-fingerprint {
    width: 52px;
    height: 52px;
  }

  .field input {
    height: 64px;
    font-size: 16px;
  }

  .submit-button {
    height: 66px;
    font-size: 20px;
  }

  .safety-line {
    font-size: 12px;
    text-align: center;
  }
}
</style>
