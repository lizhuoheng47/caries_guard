<template>
  <main class="dent-login-page">
    <div class="space-bg"></div>
    <div class="grid-bg"></div>
    <div class="star-field" aria-hidden="true">
      <span v-for="p in particles" :key="p.id" :style="p.style"></span>
    </div>

    <section class="login-stage">
      <div class="hero-layout">
        <div class="brand-box">
          <div class="brand-shield">
            <svg viewBox="0 0 48 56" fill="none" aria-hidden="true">
              <path
                d="M24 3L43 10.5V27.6C43 39.4 35.7 49.3 24 53C12.3 49.3 5 39.4 5 27.6V10.5L24 3Z"
                stroke="currentColor"
                stroke-width="4"
              />
              <path
                d="M24 18.2C20.4 14.9 13.8 16.7 12.9 23.2C12.2 28.3 14.7 31.7 16.3 36.1C17.8 40.1 17.9 46.7 21.4 47.8C24 48.7 23.8 42.7 24 38.8C24.2 42.7 24 48.7 26.6 47.8C30.1 46.7 30.2 40.1 31.7 36.1C33.3 31.7 35.8 28.3 35.1 23.2C34.2 16.7 27.6 14.9 24 18.2Z"
                fill="currentColor"
              />
            </svg>
          </div>
          <div class="brand-text">龋智卫 DentAI</div>
        </div>

        <div class="hero-title">
          <h1>
            <span>龋齿</span><em>AI</em><span>智能</span><br />
            <span>医疗辅助系统</span>
          </h1>
          <p>AI分析&nbsp;&nbsp;·&nbsp;&nbsp;病灶检测&nbsp;&nbsp;·&nbsp;&nbsp;知识增强&nbsp;&nbsp;·&nbsp;&nbsp;医生复核</p>
        </div>

        <div class="holo-tooth-wrap" aria-hidden="true">
          <div class="tooth-orbit orbit-a"></div>
          <div class="tooth-orbit orbit-b"></div>
          <div class="ground-ring ring-a"></div>
          <div class="ground-ring ring-b"></div>
          <div class="ground-glow"></div>

          <svg class="holo-tooth" viewBox="0 0 360 430" fill="none">
            <defs>
              <linearGradient id="toothStroke" x1="40" y1="18" x2="310" y2="380" gradientUnits="userSpaceOnUse">
                <stop stop-color="#69FFFF" />
                <stop offset="0.42" stop-color="#00E5FF" />
                <stop offset="0.72" stop-color="#267CFF" />
                <stop offset="1" stop-color="#875CFF" />
              </linearGradient>
              <radialGradient
                id="toothFill"
                cx="0"
                cy="0"
                r="1"
                gradientUnits="userSpaceOnUse"
                gradientTransform="translate(176 132) rotate(85) scale(247 171)"
              >
                <stop stop-color="#8EFFFF" stop-opacity="0.32" />
                <stop offset="0.38" stop-color="#00E5FF" stop-opacity="0.16" />
                <stop offset="1" stop-color="#111B65" stop-opacity="0.06" />
              </radialGradient>
              <filter id="toothGlow" x="-60" y="-60" width="480" height="560" filterUnits="userSpaceOnUse">
                <feGaussianBlur stdDeviation="8" result="blur" />
                <feColorMatrix in="blur" type="matrix" values="0 0 0 0 0 0 0 0 0 0.82 0 0 0 0 1 0 0 0 0.9 0" />
                <feMerge>
                  <feMergeNode />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            <path
              class="tooth-core-path"
              d="M180 51C143 13 72 32 58 101C49 145 74 181 86 217C99 257 98 341 134 357C162 369 160 282 177 252C194 282 193 369 221 357C257 341 256 257 269 217C281 181 306 145 297 101C283 32 217 13 180 51Z"
              fill="url(#toothFill)"
              stroke="url(#toothStroke)"
              stroke-width="6"
              filter="url(#toothGlow)"
            />
            <path d="M82 127C121 92 174 133 174 133C174 133 222 92 273 126" stroke="rgba(255,255,255,.68)" stroke-width="4" stroke-linecap="round" />
            <path d="M83 183C116 211 148 214 177 199C210 218 246 209 275 181" stroke="rgba(0,229,255,.48)" stroke-width="3" stroke-linecap="round" />

            <g class="network-lines" stroke="rgba(95,220,255,.52)" stroke-width="1.35">
              <path d="M97 105L242 76L274 147L169 209L116 168L221 326" />
              <path d="M123 61L170 208L88 248L196 294L263 215" />
              <path d="M64 153L171 128L294 104" />
              <path d="M108 305L184 212L248 328" />
              <path d="M78 215L236 168L282 247" />
              <path d="M142 349L176 251L216 351" />
            </g>
            <g class="network-dots" fill="#00E5FF">
              <circle cx="97" cy="105" r="3.5" />
              <circle cx="242" cy="76" r="3.5" />
              <circle cx="274" cy="147" r="3.5" />
              <circle cx="169" cy="209" r="4" />
              <circle cx="116" cy="168" r="3.5" />
              <circle cx="221" cy="326" r="3.5" />
              <circle cx="123" cy="61" r="3.5" />
              <circle cx="88" cy="248" r="3.5" />
              <circle cx="196" cy="294" r="3.5" />
              <circle cx="263" cy="215" r="3.5" />
              <circle cx="142" cy="349" r="3.5" />
              <circle cx="216" cy="351" r="3.5" />
            </g>
            <g stroke="rgba(121,105,255,.52)" stroke-width="2" opacity=".9">
              <path d="M126 83C170 55 214 61 251 91" />
              <path d="M88 143C125 119 142 119 180 136" />
              <path d="M179 139C214 116 245 118 280 142" />
            </g>
          </svg>
        </div>

        <div class="feature-point fp-ai">
          <div class="hex-icon">
            <svg viewBox="0 0 28 28" fill="none"><path d="M3 15h5l3-8 5 16 3-8h6" /></svg>
          </div>
          <span>AI分析</span>
        </div>
        <div class="feature-line line-ai"></div>

        <div class="feature-point fp-det">
          <div class="hex-icon">
            <svg viewBox="0 0 28 28" fill="none"><circle cx="14" cy="14" r="7" /><path d="M14 3v5M14 20v5M3 14h5M20 14h5" /><circle cx="14" cy="14" r="2" /></svg>
          </div>
          <span>病灶检测</span>
        </div>
        <div class="feature-line line-det"></div>

        <div class="feature-point fp-rag">
          <div class="hex-icon">
            <svg viewBox="0 0 28 28" fill="none"><path d="M6 6h7a4 4 0 0 1 4 4v12a4 4 0 0 0-4-4H6z" /><path d="M22 6h-7a4 4 0 0 0-4 4v12a4 4 0 0 1 4-4h7z" /></svg>
          </div>
          <span>知识增强</span>
        </div>
        <div class="feature-line line-rag"></div>

        <div class="feature-point fp-review">
          <div class="hex-icon">
            <svg viewBox="0 0 28 28" fill="none"><circle cx="14" cy="8" r="4" /><path d="M7 23a7 7 0 0 1 14 0" /><path d="M21.5 16.5l2 2 3-4" /></svg>
          </div>
          <span>医生复核</span>
        </div>
        <div class="feature-line line-review"></div>
      </div>

      <form class="login-card" @submit.prevent="onSubmit">
        <div class="card-glare"></div>
        <div class="card-diagonal"></div>
        <div class="fingerprint-badge">
          <svg viewBox="0 0 42 42" fill="none" aria-hidden="true">
            <path d="M13.5 21.5c0-5 3.2-8.5 7.6-8.5 4.5 0 7.6 3.4 7.6 8.5" />
            <path d="M17.5 24c0-2.5 1.4-4.7 3.7-4.7 2.4 0 3.7 2 3.7 4.7" />
            <path d="M21.2 24.2v7.2" />
            <path d="M11 27c-1.4-2-2.1-4.4-2.1-6.9 0-6.7 5.2-11.8 12.2-11.8 7.1 0 12.2 5.1 12.2 11.8" />
            <path d="M31.3 28.3c1.1-2.4 1.7-5 1.5-7.9" />
            <path d="M16.1 31.2c-.9-2.1-1.4-4.3-1.4-7" />
          </svg>
        </div>

        <h2>登录平台</h2>
        <div class="title-underline"></div>

        <label class="field field-user" :class="{ focus: focused === 'username' }">
          <svg class="field-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <circle cx="16" cy="10.5" r="5.2" />
            <path d="M6.7 26.5c1.7-6 6-9 9.3-9s7.6 3 9.3 9" />
          </svg>
          <input
            v-model="form.username"
            placeholder="用户名 / admin"
            autocomplete="username"
            :disabled="loading"
            @focus="focused = 'username'"
            @blur="focused = ''"
          />
        </label>

        <label class="field field-pass" :class="{ focus: focused === 'password' }">
          <svg class="field-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <rect x="7" y="13" width="18" height="13" rx="2.5" />
            <path d="M10.5 13V9.5a5.5 5.5 0 0 1 11 0V13" />
            <path d="M16 18v3.8" />
          </svg>
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="密码"
            autocomplete="current-password"
            :disabled="loading"
            @focus="focused = 'password'"
            @blur="focused = ''"
          />
          <button
            class="eye-btn"
            type="button"
            :disabled="loading"
            aria-label="切换密码显示"
            @click="showPassword = !showPassword"
          >
            <svg viewBox="0 0 30 30" fill="none">
              <path d="M3 15s4.5-7 12-7 12 7 12 7-4.5 7-12 7S3 15 3 15Z" />
              <circle cx="15" cy="15" r="3" />
              <path d="M5 5l20 20" />
            </svg>
          </button>
        </label>

        <a class="forgot" href="javascript:void(0)">忘记密码?</a>

        <button class="enter-btn" type="submit" :disabled="loading">
          <span>{{ loading ? '登录中' : '进入系统' }}</span>
          <i>→</i>
        </button>

        <div class="security-note">
          <svg viewBox="0 0 28 28" fill="none">
            <path d="M14 3l9 4v6.5C23 19.4 19.2 24 14 26 8.8 24 5 19.4 5 13.5V7z" />
            <path d="M10 14l2.6 2.6L18.6 10" />
          </svg>
          <span>安全访问&nbsp;&nbsp;·&nbsp;&nbsp;数据加密&nbsp;&nbsp;·&nbsp;&nbsp;隐私保护</span>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const form = reactive({
  username: 'admin',
  password: '123456',
})

const focused = ref('')
const showPassword = ref(false)
const loading = ref(false)

const particles = Array.from({ length: 58 }, (_, index) => {
  const seed = index + 1
  const left = (seed * 47) % 100
  const top = (seed * 29) % 100
  const size = seed % 9 === 0 ? 7 : seed % 5 === 0 ? 4 : 2
  const opacity = seed % 6 === 0 ? 0.55 : 0.24
  return {
    id: index,
    style: {
      left: `${left}%`,
      top: `${top}%`,
      width: `${size}px`,
      height: `${size}px`,
      opacity,
      animationDelay: `${(seed % 8) * 0.35}s`,
    },
  }
})

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
    message: `${normalized.message || '登录请求未完成。'}${traceSuffix}`,
  }
}

const onSubmit = async () => {
  if (loading.value) return

  if (!form.username.trim() || !form.password.trim()) {
    notificationStore.warning('登录信息不完整', '用户名和密码不能为空。')
    return
  }

  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
    })

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

<style scoped>
.dent-login-page {
  --text: #f2f7ff;
  width: 100vw;
  height: 100vh;
  min-width: 1540px;
  min-height: 880px;
  overflow: hidden;
  position: relative;
  padding-top: clamp(12px, 2vh, 24px);
  box-sizing: border-box;
  background: #020914;
  color: var(--text);
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", system-ui, sans-serif;
}

.space-bg,
.grid-bg,
.star-field {
  position: absolute;
  inset: 0;
}

.space-bg {
  background:
    radial-gradient(circle at 23% 87%, rgba(0, 229, 255, .16) 0%, transparent 29%),
    radial-gradient(circle at 70% 29%, rgba(55, 121, 255, .16) 0%, transparent 32%),
    radial-gradient(circle at 95% 26%, rgba(39, 246, 255, .18) 0%, transparent 20%),
    radial-gradient(circle at 45% 62%, rgba(0, 229, 255, .08) 0%, transparent 28%),
    linear-gradient(180deg, #061936 0%, #031020 48%, #020914 100%);
}

.space-bg::before {
  content: "";
  position: absolute;
  inset: -20%;
  background:
    linear-gradient(110deg, transparent 0 39%, rgba(0, 229, 255, .045) 39.2% 39.6%, transparent 39.8% 100%),
    linear-gradient(90deg, rgba(0, 0, 0, .26), transparent 28% 72%, rgba(0, 0, 0, .34));
}

.grid-bg {
  opacity: .68;
  background-image:
    linear-gradient(rgba(75, 201, 255, .044) 1px, transparent 1px),
    linear-gradient(90deg, rgba(75, 201, 255, .044) 1px, transparent 1px),
    linear-gradient(rgba(75, 201, 255, .026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(75, 201, 255, .026) 1px, transparent 1px);
  background-size: 54px 54px, 54px 54px, 18px 18px, 18px 18px;
  mask-image: radial-gradient(circle at 50% 50%, #000 0%, transparent 86%);
}

.grid-bg::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, transparent, rgba(0, 229, 255, .06), transparent),
    radial-gradient(circle at 30% 80%, rgba(0, 229, 255, .15), transparent 24%);
  opacity: .55;
}

.star-field span {
  position: absolute;
  display: block;
  border-radius: 999px;
  background: #21eaff;
  box-shadow: 0 0 14px rgba(0, 229, 255, .95), 0 0 30px rgba(55, 121, 255, .45);
  animation: starPulse 4.2s ease-in-out infinite;
}

.login-stage {
  position: relative;
  width: 1541px;
  height: 860px;
  margin: 0 auto;
  z-index: 1;
}

.hero-layout {
  position: absolute;
  left: 0;
  top: 0;
  width: 860px;
  height: 860px;
}

.brand-box {
  position: absolute;
  left: 18px;
  top: 18px;
  width: 354px;
  height: 98px;
  border-radius: 16px;
  border: 2px solid rgba(112, 204, 255, .32);
  background: linear-gradient(180deg, rgba(25, 52, 87, .76), rgba(15, 30, 57, .58));
  box-shadow: inset 0 0 25px rgba(0, 229, 255, .08), 0 0 22px rgba(0, 229, 255, .13);
  display: flex;
  align-items: center;
  padding-left: 20px;
  gap: 19px;
}

.brand-shield {
  width: 61px;
  height: 72px;
  display: grid;
  place-items: center;
  color: #45fff6;
  filter: drop-shadow(0 0 12px rgba(39, 246, 255, .7));
}

.brand-shield svg {
  width: 60px;
  height: 70px;
}

.brand-text {
  font-size: 31px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 1px;
  color: #f7fbff;
  text-shadow: 0 0 10px rgba(255, 255, 255, .24);
}

.hero-title {
  position: absolute;
  left: 30px;
  top: 165px;
  width: 780px;
}

.hero-title h1 {
  margin: 0;
  font-size: 74px;
  line-height: 1.33;
  font-weight: 900;
  letter-spacing: 2px;
  color: white;
  text-shadow: 0 0 18px rgba(255, 255, 255, .16), 0 0 28px rgba(61, 118, 255, .12);
}

.hero-title h1 em {
  font-style: normal;
  padding: 0 12px;
  color: #33f7ff;
  letter-spacing: 6px;
  text-shadow: 0 0 18px rgba(0, 229, 255, .72);
}

.hero-title p {
  margin: 12px 0 0 3px;
  color: rgba(218, 230, 249, .68);
  font-size: 28px;
  font-weight: 500;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 0, 0, .4);
}

.holo-tooth-wrap {
  position: absolute;
  left: 150px;
  top: 434px;
  width: 360px;
  height: 370px;
}

.holo-tooth {
  position: absolute;
  left: 0;
  top: -40px;
  width: 360px;
  height: 430px;
  z-index: 3;
  filter: drop-shadow(0 0 12px rgba(39, 246, 255, .75));
}

.tooth-core-path {
  stroke-dasharray: 11 7;
  animation: dashFlow 16s linear infinite;
}

.network-lines,
.network-dots {
  filter: drop-shadow(0 0 8px rgba(0, 229, 255, .68));
}

.tooth-orbit {
  position: absolute;
  left: 18px;
  top: 82px;
  width: 320px;
  height: 116px;
  border: 2px solid rgba(0, 229, 255, .48);
  border-radius: 50%;
  box-shadow: 0 0 16px rgba(0, 229, 255, .42), inset 0 0 18px rgba(0, 229, 255, .14);
  z-index: 1;
}

.orbit-a {
  transform: rotate(-8deg);
  animation: orbitFloat 9s ease-in-out infinite;
}

.orbit-b {
  top: 100px;
  border-color: rgba(104, 92, 255, .46);
  transform: rotate(17deg);
  animation: orbitFloat2 11s ease-in-out infinite;
}

.ground-ring {
  position: absolute;
  left: 3px;
  bottom: 12px;
  width: 352px;
  height: 70px;
  border-radius: 50%;
  border: 2px solid rgba(0, 229, 255, .36);
  box-shadow: 0 0 20px rgba(0, 229, 255, .35), inset 0 0 14px rgba(0, 229, 255, .18);
  z-index: 1;
}

.ring-b {
  left: 39px;
  bottom: 25px;
  width: 276px;
  height: 42px;
  opacity: .55;
}

.ground-glow {
  position: absolute;
  left: 33px;
  bottom: 25px;
  width: 295px;
  height: 52px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(0, 229, 255, .78), rgba(0, 229, 255, .25) 42%, transparent 72%);
  filter: blur(5px);
  z-index: 0;
}

.feature-point {
  position: absolute;
  width: 126px;
  height: 122px;
  text-align: center;
  z-index: 5;
}

.hex-icon {
  width: 68px;
  height: 68px;
  margin: 0 auto 9px;
  display: grid;
  place-items: center;
  clip-path: polygon(25% 6%, 75% 6%, 100% 50%, 75% 94%, 25% 94%, 0 50%);
  background: linear-gradient(180deg, rgba(28, 70, 116, .82), rgba(11, 31, 67, .78));
  border: 1px solid rgba(116, 205, 255, .38);
  box-shadow: inset 0 0 16px rgba(0, 229, 255, .18), 0 0 18px rgba(0, 98, 255, .25);
  position: relative;
}

.hex-icon::before {
  content: "";
  position: absolute;
  inset: 2px;
  clip-path: inherit;
  border: 1px solid rgba(82, 202, 255, .32);
}

.hex-icon svg {
  width: 35px;
  height: 35px;
  stroke: #a7e9ff;
  stroke-width: 2.3;
  stroke-linecap: round;
  stroke-linejoin: round;
  filter: drop-shadow(0 0 7px rgba(0, 229, 255, .5));
}

.feature-point span {
  color: rgba(223, 235, 255, .66);
  font-size: 17px;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(0, 0, 0, .5);
}

.fp-ai { left: 0; top: 520px; }
.fp-det { left: 48px; top: 654px; }
.fp-rag { left: 596px; top: 520px; }
.fp-review { left: 548px; top: 654px; }

.feature-line {
  position: absolute;
  height: 2px;
  background: linear-gradient(90deg, rgba(74, 207, 255, .42), rgba(74, 207, 255, .08));
  box-shadow: 0 0 8px rgba(0, 229, 255, .22);
  z-index: 2;
}

.line-ai { left: 92px; top: 558px; width: 120px; }
.line-det { left: 152px; top: 700px; width: 98px; transform: rotate(-18deg); }
.line-rag { left: 520px; top: 558px; width: 118px; }
.line-review { left: 512px; top: 700px; width: 92px; transform: rotate(18deg); }

.login-card {
  position: absolute;
  right: 0;
  top: 96px;
  width: 681px;
  height: 717px;
  border-radius: 28px;
  border: 2px solid rgba(112, 224, 255, .62);
  background:
    radial-gradient(circle at 87% 9%, rgba(0, 229, 255, .28), transparent 28%),
    linear-gradient(142deg, rgba(47, 84, 126, .54), rgba(15, 31, 63, .52) 45%, rgba(10, 18, 40, .78) 100%);
  box-shadow:
    inset 0 0 48px rgba(143, 191, 255, .12),
    inset 0 0 160px rgba(0, 229, 255, .06),
    0 0 20px rgba(0, 229, 255, .18),
    0 30px 80px rgba(0, 0, 0, .42);
  backdrop-filter: blur(24px);
  overflow: hidden;
}

.card-glare {
  position: absolute;
  left: -40px;
  top: -30px;
  width: 270px;
  height: 270px;
  background: radial-gradient(circle, rgba(255,255,255,.16), transparent 65%);
  opacity: .35;
}

.card-diagonal {
  position: absolute;
  right: 92px;
  top: -66px;
  width: 2px;
  height: 315px;
  background: linear-gradient(180deg, transparent, rgba(153, 216, 255, .18), transparent);
  transform: rotate(18deg);
}

.fingerprint-badge {
  position: absolute;
  right: 48px;
  top: 58px;
  width: 78px;
  height: 78px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(69, 235, 255, .35);
  background: rgba(0, 229, 255, .1);
  box-shadow: inset 0 0 20px rgba(0, 229, 255, .2), 0 0 22px rgba(0, 229, 255, .28);
}

.fingerprint-badge svg {
  width: 48px;
  height: 48px;
  stroke: #35f8ff;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  filter: drop-shadow(0 0 8px rgba(0, 229, 255, .65));
}

.login-card h2 {
  position: absolute;
  left: 67px;
  top: 73px;
  margin: 0;
  font-size: 39px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: 2px;
  color: #f7fbff;
  text-shadow: 0 0 12px rgba(255, 255, 255, .18);
}

.title-underline {
  position: absolute;
  left: 69px;
  top: 142px;
  width: 56px;
  height: 5px;
  border-radius: 99px;
  background: linear-gradient(90deg, #37f8ff, rgba(55, 248, 255, 0));
  box-shadow: 0 0 15px rgba(0, 229, 255, .65);
}

.field {
  position: absolute;
  left: 63px;
  width: 555px;
  height: 86px;
  border-radius: 16px;
  border: 2px solid rgba(62, 201, 238, .35);
  background: rgba(18, 41, 75, .55);
  box-shadow: inset 0 0 32px rgba(0, 229, 255, .035), 0 0 0 rgba(0,0,0,0);
  display: flex;
  align-items: center;
  transition: border-color .2s ease, box-shadow .2s ease, background .2s ease;
}

.field.focus {
  border-color: rgba(57, 244, 255, .78);
  background: rgba(19, 45, 82, .72);
  box-shadow: 0 0 20px rgba(0, 229, 255, .2), inset 0 0 28px rgba(0, 229, 255, .06);
}

.field-user { top: 190px; }
.field-pass { top: 309px; }

.field-icon {
  width: 34px;
  height: 34px;
  margin-left: 25px;
  margin-right: 22px;
  stroke: rgba(197, 221, 252, .62);
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.field input {
  flex: 1;
  height: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #edf7ff;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: .5px;
}

.field input::placeholder {
  color: rgba(223, 235, 255, .67);
}

.field input:disabled {
  cursor: not-allowed;
}

.eye-btn {
  width: 52px;
  height: 52px;
  margin-right: 15px;
  border: 0;
  outline: 0;
  background: transparent;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.eye-btn:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.eye-btn svg {
  width: 31px;
  height: 31px;
  stroke: rgba(197, 221, 252, .46);
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.forgot {
  position: absolute;
  right: 61px;
  top: 429px;
  color: rgba(224, 234, 255, .62);
  font-size: 17px;
  font-weight: 700;
  text-decoration: none;
}

.forgot:hover {
  color: #37f8ff;
}

.enter-btn {
  position: absolute;
  left: 63px;
  top: 494px;
  width: 555px;
  height: 91px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(100deg, #35fff4 0%, #21cdfd 36%, #3f79ff 66%, #7856ff 100%);
  box-shadow:
    0 0 28px rgba(0, 229, 255, .66),
    0 0 54px rgba(90, 86, 255, .48),
    inset 0 0 18px rgba(255, 255, 255, .25);
  color: #f7fbff;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 2px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  transition: transform .18s ease, filter .18s ease;
}

.enter-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.08);
}

.enter-btn:disabled {
  opacity: .75;
  cursor: wait;
}

.enter-btn i {
  font-style: normal;
  font-size: 40px;
  font-weight: 500;
  line-height: 1;
  transform: translateY(-1px);
}

.security-note {
  position: absolute;
  left: 154px;
  bottom: 43px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(213, 228, 251, .63);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: .5px;
}

.security-note svg {
  width: 28px;
  height: 28px;
  stroke: rgba(198, 223, 255, .68);
  stroke-width: 2.1;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@keyframes starPulse {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); opacity: .22; }
  50% { transform: translate3d(0, -13px, 0) scale(1.35); opacity: .78; }
}

@keyframes dashFlow {
  from { stroke-dashoffset: 0; }
  to { stroke-dashoffset: -360; }
}

@keyframes orbitFloat {
  0%, 100% { transform: rotate(-8deg) translateY(0); opacity: .75; }
  50% { transform: rotate(-11deg) translateY(-5px); opacity: 1; }
}

@keyframes orbitFloat2 {
  0%, 100% { transform: rotate(17deg) translateY(0); opacity: .65; }
  50% { transform: rotate(20deg) translateY(4px); opacity: .95; }
}
</style>
