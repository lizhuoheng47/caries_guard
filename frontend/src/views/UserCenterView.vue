<template>
  <div class="user-center">
    <header class="page-header">
      <div>
        <h1>用户中心</h1>
        <p class="subtitle">专注口腔健康 · 智能精准诊断</p>
      </div>
    </header>

    <!-- Hero card -->
    <div class="hero-card">
      <svg class="hero-net" viewBox="0 0 800 240" preserveAspectRatio="none">
        <g stroke="rgba(0,229,255,.08)" stroke-width="1" fill="none">
          <path d="M0 60 Q200 20 400 90 T800 60"/>
          <path d="M0 140 Q220 200 440 130 T800 180"/>
          <path d="M0 200 Q300 120 500 220 T800 130"/>
        </g>
      </svg>

      <div class="hero-left">
        <div class="avatar-ring">
          <div class="avatar-orbit"></div>
          <div class="avatar-inner">
            <svg viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="24" r="11" stroke="currentColor" stroke-width="2"/>
              <path d="M12 56c2.6-9 10-13 20-13s17.4 4 20 13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </div>

        <div class="hero-meta">
          <div class="hello">欢迎回来，<span class="name">{{ displayName }}</span></div>
          <div class="role">
            <span class="role-tag">{{ userTypeLabel }}</span>
            <span class="role-tag role-tag-soft">{{ primaryRoleLabel }}</span>
            <span class="role-tag role-tag-soft">工号 {{ user.userNo || '--' }}</span>
          </div>
          <div class="motto">
            <span class="motto-line"></span>
            当前展示信息均来自后端用户档案
          </div>
        </div>
      </div>

      <div class="hero-divider"></div>

      <div class="hero-right">
        <div class="shield-wrap">
          <div class="shield-orbit shield-orbit-a"></div>
          <div class="shield-orbit shield-orbit-b"></div>
          <div class="shield-glow"></div>
          <svg class="shield-icon" viewBox="0 0 64 76" fill="none">
            <path d="M32 4l24 9v18c0 16-10 28-24 33C18 59 8 47 8 31V13l24-9Z" stroke="#35f8ff" stroke-width="2" fill="rgba(0,229,255,0.06)"/>
            <path d="M22 36l7 7 13-15" stroke="#35f8ff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <div class="shield-ground"></div>
        </div>
        <div class="security">
          <div class="sec-cap">账户状态</div>
          <div class="sec-level">{{ accountStatusLabel }}</div>
          <div class="sec-time">上次登录：{{ formatDateTime(user.lastLoginAt) }}</div>
          <div class="sec-btn">机构 ID {{ user.orgId }}</div>
        </div>
      </div>
    </div>

    <div class="row-2col">
      <div class="card summary-card">
        <div class="card-head">
          <div class="card-title"><span class="title-bar"></span>账号摘要</div>
          <span class="head-sub">来自后端 /auth/me</span>
        </div>
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.k" class="summary-item">
            <div class="summary-label">{{ item.k }}</div>
            <div class="summary-value">{{ item.v }}</div>
          </div>
        </div>
      </div>

      <div class="card timeline-card">
        <div class="card-head">
          <div class="card-title"><span class="title-bar"></span>登录与安全</div>
          <span class="head-sub">实时用户标识</span>
        </div>
        <ul class="info-list">
          <li v-for="item in securityItems" :key="item.k" class="info-item">
            <span class="info-key">{{ item.k }}</span>
            <span class="info-val">{{ item.v }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="card profile-card">
      <div class="card-head">
        <div class="card-title"><span class="title-bar"></span>个人资料</div>
        <span class="head-sub">数据库脱敏展示</span>
      </div>
      <div class="profile-grid">
        <div v-for="f in profile" :key="f.k" class="profile-field">
          <div class="pf-label">{{ f.k }}</div>
          <div class="pf-value">{{ f.v }}</div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { User } from '@/models/auth'

const authStore = useAuthStore()

const emptyUser: User = {
  id: 0,
  username: '--',
  nickname: '--',
  realNameMasked: '--',
  deptId: undefined,
  userNo: '--',
  phoneMasked: '--',
  emailMasked: '--',
  avatarUrl: '',
  roles: [],
  orgId: 0,
  userTypeCode: undefined,
  genderCode: undefined,
  certificateNoMasked: '--',
  lastLoginAt: undefined,
  status: undefined,
}

const user = computed(() => authStore.user ?? emptyUser)

const mapUserTypeLabel = (value?: string) => {
  if (value === 'ADMIN') return '系统管理员'
  if (value === 'ORG_ADMIN') return '机构管理员'
  if (value === 'DOCTOR') return '医生'
  if (value === 'SCREENER') return '筛查员'
  if (value === 'PATIENT') return '患者'
  return value || '--'
}

const mapGenderLabel = (value?: string) => {
  if (value === 'MALE') return '男'
  if (value === 'FEMALE') return '女'
  if (value === 'UNKNOWN') return '未知'
  return value || '--'
}

const mapRoleLabel = (value?: string) => {
  if (value === 'SYS_ADMIN' || value === 'ADMIN' || value === 'ROLE_ADMIN') return '系统管理员'
  if (value === 'ORG_ADMIN' || value === 'ROLE_ORG_ADMIN') return '机构管理员'
  if (value === 'DOCTOR' || value === 'ROLE_DOCTOR') return '医生'
  if (value === 'SCREENER' || value === 'ROLE_SCREENER') return '筛查员'
  return value || '--'
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const displayName = computed(() => user.value.realNameMasked || user.value.nickname || user.value.username || '--')
const userTypeLabel = computed(() => mapUserTypeLabel(user.value.userTypeCode))
const primaryRoleLabel = computed(() => mapRoleLabel(user.value.roles?.[0]))
const accountStatusLabel = computed(() => {
  if (user.value.status === 'ACTIVE') return '正常'
  if (user.value.status === 'DISABLED') return '禁用'
  return user.value.status || '--'
})

const summaryItems = computed(() => [
  { k: '登录账号', v: user.value.username || '--' },
  { k: '展示名称', v: displayName.value },
  { k: '用户类型', v: userTypeLabel.value },
  { k: '主角色', v: primaryRoleLabel.value },
])

const securityItems = computed(() => [
  { k: '用户 ID', v: String(user.value.id || '--') },
  { k: '机构 ID', v: String(user.value.orgId || '--') },
  { k: '部门 ID', v: user.value.deptId ? String(user.value.deptId) : '--' },
  { k: '上次登录', v: formatDateTime(user.value.lastLoginAt) },
])

const profile = computed(() => [
  { k: '姓名', v: displayName.value },
  { k: '昵称', v: user.value.nickname || '--' },
  { k: '工号', v: user.value.userNo || '--' },
  { k: '联系电话', v: user.value.phoneMasked || '--' },
  { k: '电子邮箱', v: user.value.emailMasked || '--' },
  { k: '性别', v: mapGenderLabel(user.value.genderCode) },
  { k: '证件号', v: user.value.certificateNoMasked || '--' },
  { k: '角色列表', v: user.value.roles?.length ? user.value.roles.map(mapRoleLabel).join(' / ') : '--' },
])

</script>

<style scoped>
.user-center { display: flex; flex-direction: column; gap: 18px; }

.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 4px; }
.page-header h1 { margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 1px; color: #f7fbff; }
.subtitle { margin: 6px 0 0; font-size: 12px; color: #6f86b6; letter-spacing: 1px; }

.bell { position: relative; width: 40px; height: 40px; border-radius: 10px; border: 1px solid rgba(112, 224, 255, 0.18); background: rgba(15, 31, 63, 0.65); color: #c5d8f7; cursor: pointer; display: grid; place-items: center; }
.bell svg { width: 18px; height: 18px; }
.bell-dot { position: absolute; top: 9px; right: 11px; width: 6px; height: 6px; border-radius: 50%; background: #ff636e; box-shadow: 0 0 8px #ff636e; }

/* Hero */
.hero-card {
  position: relative;
  padding: 24px 28px;
  border-radius: 16px;
  border: 1px solid rgba(112, 224, 255, 0.16);
  background:
    radial-gradient(ellipse at 70% 50%, rgba(0,229,255,0.08), transparent 50%),
    linear-gradient(180deg, rgba(18, 41, 75, 0.85), rgba(10, 18, 40, 0.92));
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 24px;
  overflow: hidden;
  min-height: 200px;
}
.hero-net { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0.6; pointer-events: none; }

.hero-left { display: flex; align-items: center; gap: 22px; position: relative; }

.avatar-ring {
  position: relative;
  width: 110px; height: 110px;
  border-radius: 50%;
  border: 2px solid rgba(0, 229, 255, 0.55);
  background: rgba(0, 229, 255, 0.08);
  display: grid; place-items: center;
  box-shadow: 0 0 24px rgba(0, 229, 255, 0.25), inset 0 0 18px rgba(0, 229, 255, 0.15);
}
.avatar-orbit { position: absolute; inset: -8px; border-radius: 50%; border: 1px dashed rgba(0, 229, 255, 0.4); animation: spin 18s linear infinite; }
.avatar-inner { width: 88px; height: 88px; border-radius: 50%; display: grid; place-items: center; color: #35f8ff; background: rgba(0, 0, 0, 0.25); }
.avatar-inner svg { width: 50px; height: 50px; }
.avatar-edit { position: absolute; bottom: 2px; right: 4px; width: 26px; height: 26px; border-radius: 50%; border: 1px solid rgba(0, 229, 255, 0.6); background: #061936; color: #35f8ff; display: grid; place-items: center; cursor: pointer; }
.avatar-edit svg { width: 12px; height: 12px; }

.hero-meta { display: flex; flex-direction: column; gap: 8px; }
.hello { font-size: 28px; font-weight: 800; color: #f7fbff; letter-spacing: 1px; }
.hello .name { color: #35f8ff; text-shadow: 0 0 14px rgba(0, 229, 255, 0.5); }

.role { display: flex; gap: 6px; flex-wrap: wrap; }
.role-tag { padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; color: #35f8ff; background: rgba(0, 229, 255, 0.12); border: 1px solid rgba(0, 229, 255, 0.4); }
.role-tag-soft { color: #c5d8f7; background: rgba(112, 224, 255, 0.05); border-color: rgba(112, 224, 255, 0.2); }

.motto { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #6f86b6; margin-top: 4px; }
.motto-line { width: 26px; height: 1px; background: rgba(112, 224, 255, 0.4); }

.hero-divider { width: 1px; height: 130px; background: linear-gradient(180deg, transparent, rgba(112, 224, 255, 0.3), transparent); }

.hero-right { display: flex; align-items: center; gap: 20px; }
.shield-wrap { position: relative; width: 130px; height: 130px; display: grid; place-items: center; }
.shield-orbit { position: absolute; inset: 0; border: 1px solid rgba(0, 229, 255, 0.3); border-radius: 50%; }
.shield-orbit-a { animation: spin 16s linear infinite; }
.shield-orbit-b { inset: 12px; border-style: dashed; opacity: 0.5; animation: spin 22s linear infinite reverse; }
.shield-glow { position: absolute; inset: 18px; border-radius: 50%; background: radial-gradient(circle, rgba(0,229,255,0.3), transparent 70%); }
.shield-icon { position: relative; width: 60px; height: 70px; filter: drop-shadow(0 0 12px rgba(0, 229, 255, 0.6)); }
.shield-ground { position: absolute; bottom: -16px; left: 50%; transform: translateX(-50%); width: 130px; height: 28px; border-radius: 50%; background: radial-gradient(ellipse, rgba(0,229,255,0.5), transparent 70%); filter: blur(4px); }

.security { display: flex; flex-direction: column; gap: 6px; min-width: 140px; }
.sec-cap { font-size: 12px; color: #c5d8f7; }
.sec-level { font-size: 32px; font-weight: 800; color: #35f8ff; letter-spacing: 4px; text-shadow: 0 0 14px rgba(0, 229, 255, 0.55); line-height: 1; }
.sec-time { font-size: 11px; color: #6f86b6; }
.sec-btn { align-self: flex-start; margin-top: 8px; padding: 6px 14px 6px 16px; display: inline-flex; align-items: center; gap: 6px; border-radius: 18px; border: 1px solid rgba(0, 229, 255, 0.4); background: rgba(0, 229, 255, 0.08); color: #35f8ff; font-size: 12px; font-weight: 600; cursor: pointer; }
.sec-btn svg { width: 12px; height: 12px; }

/* Two-col row */
.row-2col { display: grid; grid-template-columns: 1.2fr 1fr; gap: 14px; }

/* Card baseline */
.card { position: relative; border-radius: 14px; border: 1px solid rgba(112, 224, 255, 0.1); background: linear-gradient(180deg, rgba(18, 41, 75, 0.7), rgba(10, 18, 40, 0.85)); padding: 18px 20px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.card-title { display: flex; align-items: center; gap: 10px; font-size: 14px; font-weight: 700; color: #f2f7ff; letter-spacing: 1px; }
.title-bar { width: 3px; height: 14px; border-radius: 3px; background: #35f8ff; box-shadow: 0 0 8px #35f8ff; }
.head-sub { font-size: 11px; color: #6f86b6; }
.head-link { font-size: 11px; color: #35f8ff; text-decoration: none; }

.summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 10px; border: 1px solid rgba(112, 224, 255, 0.08); background: rgba(15, 31, 63, 0.45); }
.summary-label { font-size: 11px; color: #6f86b6; letter-spacing: 1px; }
.summary-value { margin-top: 6px; font-size: 18px; font-weight: 700; color: #f7fbff; }

.info-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.info-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; border-radius: 10px; background: rgba(15, 31, 63, 0.45); border: 1px solid rgba(112, 224, 255, 0.08); }
.info-key { font-size: 12px; color: #6f86b6; }
.info-val { font-size: 13px; color: #f2f7ff; font-weight: 600; text-align: right; }

/* Profile */
.profile-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px 22px; }
.profile-field { display: flex; flex-direction: column; gap: 4px; padding: 4px 0; border-bottom: 1px dashed rgba(112, 224, 255, 0.08); }
.pf-label { font-size: 11px; color: #6f86b6; letter-spacing: 1px; }
.pf-value { font-size: 13px; color: #f2f7ff; font-weight: 600; }

/* Actions */
.actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.action {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid color-mix(in oklab, var(--tone) 22%, transparent);
  background: linear-gradient(135deg, color-mix(in oklab, var(--tone) 8%, transparent), rgba(10, 18, 40, 0.4));
  color: #f2f7ff; cursor: pointer; text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease;
}
.action:hover { transform: translateY(-2px); border-color: color-mix(in oklab, var(--tone) 55%, transparent); }
.tone-mint   { --tone: #35f8ff; }
.tone-violet { --tone: #9b6bff; }
.tone-cyan   { --tone: #3f79ff; }
.tone-amber  { --tone: #f7a23a; }

.action-icon {
  width: 38px; height: 38px;
  border-radius: 8px;
  display: grid; place-items: center;
  background: rgba(0,0,0,0.3);
  border: 1px solid color-mix(in oklab, var(--tone) 40%, transparent);
  color: var(--tone);
  flex-shrink: 0;
}
.action-icon svg, .action-icon :deep(svg) { width: 20px; height: 20px; }

.action-text { display: flex; flex-direction: column; gap: 3px; flex: 1; min-width: 0; }
.action-title { font-size: 14px; font-weight: 700; color: #f7fbff; }
.action-sub { font-size: 11px; color: #6f86b6; }
.action-chev { width: 14px; height: 14px; color: var(--tone); flex-shrink: 0; }

@keyframes spin { to { transform: rotate(360deg); } }
</style>

