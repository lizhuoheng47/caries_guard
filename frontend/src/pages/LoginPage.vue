<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, Fingerprint, LockKeyhole, UserRound } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { ApiClientError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const wireframePoints: Array<[number, number]> = [
  [170, 112],
  [96, 179],
  [145, 356],
  [203, 402],
  [260, 314],
  [317, 402],
  [375, 356],
  [424, 179],
  [350, 112],
  [242, 138],
  [278, 138]
]

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const username = ref('admin')
const password = ref('123456')
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
  <div class="min-h-screen overflow-hidden bg-[#06111F] text-white">
    <div class="relative min-h-screen">
      <div class="absolute inset-0 opacity-[0.18]" aria-hidden="true">
        <div
          class="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.06)_1px,transparent_1px)] bg-[size:56px_56px]"
        ></div>
        <div
          class="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(2,6,23,0.18)_58%,rgba(2,6,23,0.72)_100%)]"
        ></div>
      </div>

      <div
        class="relative mx-auto grid min-h-screen max-w-[1600px] grid-cols-1 items-center gap-10 px-6 py-8 lg:grid-cols-[1.05fr_0.95fr] lg:px-10 xl:px-14"
      >
        <section class="relative flex min-h-[420px] items-center sm:min-h-[560px] lg:min-h-[720px]">
          <div class="absolute left-0 top-0 hidden h-[620px] w-[620px] lg:block xl:h-[660px] xl:w-[660px]">
            <div class="absolute inset-0 animate-fade-in-scale">
              <svg viewBox="0 0 520 520" class="h-full w-full" aria-hidden="true">
                <defs>
                  <linearGradient id="lineGlowV3" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" stop-color="#7DD3FC" />
                    <stop offset="50%" stop-color="#2DD4BF" />
                    <stop offset="100%" stop-color="#A78BFA" />
                  </linearGradient>
                  <filter id="softGlowV3">
                    <feGaussianBlur stdDeviation="6" result="blur" />
                    <feMerge>
                      <feMergeNode in="blur" />
                      <feMergeNode in="SourceGraphic" />
                    </feMerge>
                  </filter>
                </defs>

                <g opacity="0.96" filter="url(#softGlowV3)">
                  <path
                    d="M170 112C132 116 104 141 96 179C90 211 96 247 108 279C118 306 132 333 145 356C153 370 160 387 171 398C180 407 193 410 203 402C214 393 220 377 224 361C229 340 235 314 260 314C285 314 291 340 296 361C300 377 306 393 317 402C327 410 340 407 349 398C360 387 367 370 375 356C388 333 402 306 412 279C424 247 430 211 424 179C416 141 388 116 350 112C323 110 299 121 278 138C272 143 266 149 260 156C254 149 248 143 242 138C221 121 197 110 170 112Z"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-width="3.2"
                  />
                  <path
                    d="M147 162C134 184 133 214 140 243C148 276 166 311 186 343"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-opacity="0.8"
                    stroke-width="2"
                  />
                  <path
                    d="M373 162C386 184 387 214 380 243C372 276 354 311 334 343"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-opacity="0.8"
                    stroke-width="2"
                  />
                  <path
                    d="M205 139C227 160 235 188 236 216"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-opacity="0.7"
                    stroke-width="2"
                  />
                  <path
                    d="M315 139C293 160 285 188 284 216"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-opacity="0.7"
                    stroke-width="2"
                  />
                  <path
                    d="M224 332C232 318 243 309 260 309C277 309 288 318 296 332"
                    fill="none"
                    stroke="url(#lineGlowV3)"
                    stroke-opacity="0.85"
                    stroke-width="2.4"
                  />

                  <circle
                    v-for="(point, i) in wireframePoints"
                    :key="`circle-${i}`"
                    :cx="point[0]"
                    :cy="point[1]"
                    r="4.5"
                    fill="#DDFBFF"
                    opacity="0.95"
                  />
                </g>
              </svg>
            </div>
          </div>

          <div class="relative z-10 w-full max-w-3xl lg:ml-[210px] xl:ml-[270px]">
            <div
              class="mb-10 inline-flex items-start gap-4 rounded-[28px] border border-white/10 bg-white/[0.06] px-5 py-4 backdrop-blur-xl animate-fade-in-up-1"
            >
              <div
                class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-400 to-teal-400 text-slate-950 shadow-[0_0_30px_rgba(45,212,191,0.35)]"
              >
                <svg viewBox="0 0 24 24" fill="none" class="h-6 w-6" aria-hidden="true">
                  <path
                    d="M8.3 4.7C8.3 3.8 9 3 9.9 3H14.1C15 3 15.7 3.8 15.7 4.7V7.1C15.7 8.1 15.1 9 14.2 9.4L13.6 9.7V12.1C13.6 14.3 14.6 16.4 16.2 17.7L18 19.1"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M15.7 5.9H18.2C19.2 5.9 20 6.7 20 7.7V8.1"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                  <path
                    d="M8.3 5.9H5.8C4.8 5.9 4 6.7 4 7.7V8.1"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                  <path
                    d="M10.4 9.7V12.1C10.4 14.3 9.4 16.4 7.8 17.7L6 19.1"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <path d="M12 13.6V18.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <path d="M9.5 16.2H14.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </div>
              <div class="flex flex-col items-start justify-center leading-none">
                <div class="text-[18px] font-semibold tracking-normal text-white">龋智卫 DentAI</div>
                <div class="mt-2 text-[14px] uppercase tracking-normal text-white/[0.48]">
                  Clinical Intelligence Platform
                </div>
              </div>
            </div>

            <div class="relative animate-fade-in-up-2">
              <h1 class="max-w-3xl text-5xl font-semibold leading-[0.95] tracking-normal sm:text-7xl xl:text-[92px]">
                <span class="block text-white">重新定义</span>
                <span class="mt-4 block bg-gradient-to-r from-white via-cyan-200 to-teal-300 bg-clip-text text-transparent">
                  龋病影像筛查
                </span>
              </h1>
            </div>

            <p class="mt-12 pl-2 text-2xl tracking-normal text-white/70 xl:mt-24 xl:text-[30px] animate-fade-in-up-3">
              AI分析 · 知识增强 · 医生复核
            </p>
          </div>
        </section>

        <section class="flex items-center justify-center pb-8 lg:justify-end lg:pb-0">
          <form class="relative w-full max-w-[560px] animate-slide-in-right" @submit.prevent="handleLogin">
            <div
              class="absolute -inset-[1px] rounded-[34px] bg-[linear-gradient(135deg,rgba(125,211,252,0.55),rgba(45,212,191,0.18),rgba(167,139,250,0.32))] opacity-80 blur-sm"
            ></div>

            <div
              class="relative overflow-hidden rounded-[34px] border border-white/10 bg-white/[0.08] shadow-[0_30px_100px_rgba(0,0,0,0.45)] backdrop-blur-2xl"
            >
              <div
                class="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.16),transparent_30%),linear-gradient(180deg,rgba(255,255,255,0.04),rgba(255,255,255,0.02))]"
              ></div>

              <div class="relative p-8 md:p-10 xl:p-12">
                <div class="mb-10 flex items-center justify-between">
                  <div class="text-4xl font-semibold tracking-normal text-white">登录平台</div>
                  <div class="flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.08] text-cyan-200">
                    <Fingerprint class="h-7 w-7" aria-hidden="true" />
                  </div>
                </div>

                <div class="space-y-5">
                  <label class="relative block">
                    <span class="sr-only">账号 / 邮箱</span>
                    <UserRound class="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-white/[0.38]" />
                    <input
                      v-model.trim="username"
                      type="text"
                      autocomplete="username"
                      placeholder="账号 / 邮箱"
                      :disabled="loading"
                      class="flex h-[72px] w-full rounded-2xl border border-white/10 bg-black/[0.15] pl-14 pr-5 text-xl text-white placeholder:text-white/[0.28] transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300 disabled:cursor-not-allowed disabled:opacity-70"
                    />
                  </label>

                  <label class="relative block">
                    <span class="sr-only">密码</span>
                    <LockKeyhole class="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-white/[0.38]" />
                    <input
                      v-model="password"
                      type="password"
                      autocomplete="current-password"
                      placeholder="密码"
                      :disabled="loading"
                      class="flex h-[72px] w-full rounded-2xl border border-white/10 bg-black/[0.15] pl-14 pr-5 text-xl text-white placeholder:text-white/[0.28] transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300 disabled:cursor-not-allowed disabled:opacity-70"
                    />
                  </label>
                </div>

                <div class="mt-5 flex justify-end">
                  <button type="button" class="text-base font-medium text-cyan-200/90 transition hover:text-cyan-100">
                    忘记密码？
                  </button>
                </div>

                <button
                  type="submit"
                  :disabled="loading"
                  class="mt-8 inline-flex h-[74px] w-full items-center justify-center rounded-2xl bg-gradient-to-r from-cyan-400 via-teal-400 to-emerald-400 text-xl font-semibold text-slate-950 shadow-[0_18px_60px_rgba(45,212,191,0.35)] transition hover:scale-[1.01] hover:brightness-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:scale-100"
                >
                  {{ loading ? '登录中...' : '进入系统' }}
                  <ArrowRight class="ml-2 h-5 w-5" aria-hidden="true" />
                </button>
              </div>
            </div>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fade-in-scale {
  from {
    opacity: 0;
    transform: scale(0.94);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-in-right {
  from {
    opacity: 0;
    transform: translateX(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

.animate-fade-in-scale {
  animation: fade-in-scale 0.8s ease-out forwards;
}

.animate-fade-in-up-1 {
  animation: fade-in-up 0.55s ease-out forwards;
}

.animate-fade-in-up-2 {
  opacity: 0;
  animation: fade-in-up 0.65s ease-out 0.08s forwards;
}

.animate-fade-in-up-3 {
  opacity: 0;
  animation: fade-in-up 0.65s ease-out 0.16s forwards;
}

.animate-slide-in-right {
  opacity: 0;
  animation: slide-in-right 0.7s ease-out 0.12s forwards;
}
</style>
