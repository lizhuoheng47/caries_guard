<template>
  <div class="user-center">
    <header class="page-header">
      <div>
        <h1>用户中心</h1>
        <p class="subtitle">专注口腔健康 · 智能精准诊断</p>
      </div>
      <button class="bell" aria-label="通知">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
          <path d="M10 19a2 2 0 0 0 4 0" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
        </svg>
        <span class="bell-dot"></span>
      </button>
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
          <button class="avatar-edit" aria-label="编辑头像">
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M11 2l3 3-8 8H3v-3l8-8Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <div class="hero-meta">
          <div class="hello">欢迎回来，<span class="name">陈医生</span></div>
          <div class="role">
            <span class="role-tag">口腔影像科</span>
            <span class="role-tag role-tag-soft">主任医师</span>
            <span class="role-tag role-tag-soft">执业证 2018</span>
          </div>
          <div class="motto">
            <span class="motto-line"></span>
            用科技守护每一份微笑
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
          <div class="sec-cap">账户安全等级</div>
          <div class="sec-level">安全</div>
          <div class="sec-time">上次登录：2024-06-01 14:30</div>
          <button class="sec-btn">
            安全中心
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Two-column: 成就 + 团队/活动 -->
    <div class="row-2col">
      <!-- Achievements -->
      <div class="card achv-card">
        <div class="card-head">
          <div class="card-title"><span class="title-bar"></span>个人成就</div>
          <span class="head-sub">已获 6 / 12</span>
        </div>
        <div class="achv-grid">
          <div v-for="a in achievements" :key="a.key" class="achv-item" :class="{ locked: a.locked }">
            <div class="achv-medal" :class="`m-${a.tone}`">
              <span v-html="a.icon"></span>
            </div>
            <div class="achv-name">{{ a.name }}</div>
            <div class="achv-cap">{{ a.cap }}</div>
          </div>
        </div>
      </div>

      <!-- Activity timeline -->
      <div class="card timeline-card">
        <div class="card-head">
          <div class="card-title"><span class="title-bar"></span>近期动态</div>
          <a class="head-link" href="javascript:void(0)">查看全部</a>
        </div>
        <ul class="timeline">
          <li v-for="(t, i) in timeline" :key="i" :class="`t-${t.tone}`">
            <span class="t-dot"></span>
            <div class="t-body">
              <div class="t-text">{{ t.text }}</div>
              <div class="t-time">{{ t.time }}</div>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- Profile detail card -->
    <div class="card profile-card">
      <div class="card-head">
        <div class="card-title"><span class="title-bar"></span>个人资料</div>
        <button class="edit-btn">
          <svg viewBox="0 0 14 14" fill="none">
            <path d="M9 2l3 3-7 7H2V9l7-7Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
          </svg>
          编辑
        </button>
      </div>
      <div class="profile-grid">
        <div v-for="f in profile" :key="f.k" class="profile-field">
          <div class="pf-label">{{ f.k }}</div>
          <div class="pf-value">{{ f.v }}</div>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="card actions-card">
      <div class="card-head">
        <div class="card-title"><span class="title-bar"></span>快捷操作</div>
      </div>
      <div class="actions">
        <button v-for="a in actions" :key="a.key" class="action" :class="`tone-${a.tone}`">
          <span class="action-icon" v-html="a.icon"></span>
          <span class="action-text">
            <span class="action-title">{{ a.title }}</span>
            <span class="action-sub">{{ a.sub }}</span>
          </span>
          <svg class="action-chev" viewBox="0 0 16 16" fill="none">
            <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const achievements = [
  { key: 'a1', name: '诊断新手', cap: '完成 100 例', tone: 'mint', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M12 2l3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1 3-6Z" fill="currentColor"/></svg>` },
  { key: 'a2', name: '精准诊断', cap: '准确率 ≥95%', tone: 'cyan', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.7"/><circle cx="12" cy="12" r="4" fill="currentColor"/></svg>` },
  { key: 'a3', name: '勤勉医者', cap: '连续 30 天', tone: 'amber', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M12 3a4 4 0 0 1 4 4c0 4-4 7-4 7s-4-3-4-7a4 4 0 0 1 4-4Z" fill="currentColor"/><path d="M5 21c2-3 5-4 7-4s5 1 7 4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" fill="none"/></svg>` },
  { key: 'a4', name: '专家审核', cap: '复核 500 例', tone: 'violet', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M4 7l8-4 8 4-8 4-8-4Z" fill="currentColor"/><path d="M4 12l8 4 8-4M4 17l8 4 8-4" stroke="currentColor" stroke-width="1.7" fill="none"/></svg>` },
  { key: 'a5', name: '夜诊达人', cap: '夜间值班 50 次', tone: 'mint', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M19 14a8 8 0 1 1-9-9 6 6 0 0 0 9 9Z" fill="currentColor"/></svg>` },
  { key: 'a6', name: '影像大师', cap: '完成 1000 例', tone: 'cyan', locked: false, icon: `<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.7"/><circle cx="9" cy="11" r="2" fill="currentColor"/><path d="M3 17l5-4 4 3 4-5 5 5" stroke="currentColor" stroke-width="1.7" fill="none" stroke-linejoin="round"/></svg>` },
  { key: 'a7', name: '协作之星', cap: '团队会诊 20 次', tone: 'amber', locked: true, icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="8" cy="9" r="3" stroke="currentColor" stroke-width="1.7"/><circle cx="16" cy="9" r="3" stroke="currentColor" stroke-width="1.7"/><path d="M3 19c.5-3 2.5-5 5-5s4.5 2 5 5M13 19c.5-3 2.5-5 5-5s4.5 2 5 5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" fill="none"/></svg>` },
  { key: 'a8', name: '科研先锋', cap: '发表论文 5 篇', tone: 'violet', locked: true, icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M6 3h9l4 4v14H6V3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M14 3v5h5" stroke="currentColor" stroke-width="1.7"/></svg>` },
]

const timeline = [
  { tone: 'mint',   text: '完成全景片诊断 · 患者王**', time: '今天 14:30' },
  { tone: 'amber',  text: '复核高风险病例 · 患者张**', time: '今天 11:20' },
  { tone: 'cyan',   text: '导出本周诊断数据报表', time: '昨天 18:05' },
  { tone: 'violet', text: '参与多学科会诊 · 罕见病例 #287', time: '昨天 09:40' },
  { tone: 'mint',   text: '更新个人偏好设置', time: '2 天前' },
]

const profile = [
  { k: '医师姓名',  v: '陈** 主任' },
  { k: '工号',      v: 'D-20180312' },
  { k: '所属科室',  v: '口腔影像科' },
  { k: '联系电话',  v: '138****6688' },
  { k: '电子邮箱',  v: 'chen****@dentai.com' },
  { k: '执业地点',  v: '上海市第一人民医院' },
  { k: '加入时间',  v: '2018-03-12' },
  { k: '账户状态',  v: '活跃' },
]

const actions = [
  { key: 'sec',    tone: 'mint',   title: '账户安全', sub: '管理账号与安全设置',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l8 3v6c0 5-3.4 9-8 11-4.6-2-8-6-8-11V6l8-3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { key: 'noti',   tone: 'violet', title: '消息通知', sub: '管理订阅与提醒',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M10 19a2 2 0 0 0 4 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>` },
  { key: 'export', tone: 'cyan',   title: '数据导出', sub: '导出我的诊断数据',
    icon: `<svg viewBox="0 0 24 24" fill="none"><path d="M7 16a4 4 0 1 1 1-7.9A6 6 0 0 1 19 11a4 4 0 0 1 0 8H7Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M12 12v6m0 0l-2-2m2 2l2-2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { key: 'pref',   tone: 'amber',  title: '偏好设置', sub: '自定义系统偏好',
    icon: `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.7"/><path d="M19 12a7 7 0 0 0-.1-1.2l2-1.5-2-3.5-2.4.9a7 7 0 0 0-2-1.2L14 3h-4l-.5 2.5a7 7 0 0 0-2 1.2l-2.4-.9-2 3.5 2 1.5A7 7 0 0 0 5 12c0 .4 0 .8.1 1.2l-2 1.5 2 3.5 2.4-.9c.6.5 1.3.9 2 1.2L10 21h4l.5-2.5c.7-.3 1.4-.7 2-1.2l2.4.9 2-3.5-2-1.5c.1-.4.1-.8.1-1.2Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>` },
]
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

/* Achievements */
.achv-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.achv-item { position: relative; padding: 14px 8px 12px; border-radius: 10px; border: 1px solid rgba(112, 224, 255, 0.08); background: rgba(15, 31, 63, 0.45); text-align: center; transition: transform .18s ease, border-color .18s ease; }
.achv-item:hover { transform: translateY(-2px); border-color: rgba(112, 224, 255, 0.25); }
.achv-item.locked { opacity: 0.45; filter: grayscale(0.6); }
.achv-medal {
  width: 44px; height: 44px;
  margin: 0 auto 8px;
  border-radius: 50%;
  display: grid; place-items: center;
  background: rgba(0,0,0,0.25);
  border: 1.5px solid var(--mt, #35f8ff);
  color: var(--mt, #35f8ff);
  filter: drop-shadow(0 0 8px var(--mt, #35f8ff));
}
.achv-medal :deep(svg), .achv-medal svg { width: 22px; height: 22px; }
.m-mint   { --mt: #35f8ff; }
.m-cyan   { --mt: #3f79ff; }
.m-amber  { --mt: #f7a23a; }
.m-violet { --mt: #9b6bff; }
.achv-name { font-size: 12px; font-weight: 700; color: #f7fbff; }
.achv-cap { font-size: 10px; color: #6f86b6; margin-top: 3px; }

/* Timeline */
.timeline { list-style: none; margin: 0; padding: 0 0 0 12px; position: relative; }
.timeline::before { content: ""; position: absolute; left: 4px; top: 4px; bottom: 4px; width: 1px; background: linear-gradient(180deg, rgba(112, 224, 255, 0.05), rgba(112, 224, 255, 0.3), rgba(112, 224, 255, 0.05)); }
.timeline li { position: relative; padding: 8px 0 12px 16px; display: flex; align-items: flex-start; gap: 10px; }
.t-dot { position: absolute; left: -3px; top: 12px; width: 9px; height: 9px; border-radius: 50%; background: var(--td, #35f8ff); box-shadow: 0 0 8px var(--td, #35f8ff); border: 2px solid #061936; }
.t-mint   { --td: #35f8ff; }
.t-cyan   { --td: #3f79ff; }
.t-amber  { --td: #f7a23a; }
.t-violet { --td: #9b6bff; }
.t-body { flex: 1; }
.t-text { font-size: 12px; color: #f2f7ff; line-height: 1.5; }
.t-time { font-size: 10px; color: #6f86b6; margin-top: 2px; }

/* Profile */
.profile-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px 22px; }
.profile-field { display: flex; flex-direction: column; gap: 4px; padding: 4px 0; border-bottom: 1px dashed rgba(112, 224, 255, 0.08); }
.pf-label { font-size: 11px; color: #6f86b6; letter-spacing: 1px; }
.pf-value { font-size: 13px; color: #f2f7ff; font-weight: 600; }

.edit-btn { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 14px; border: 1px solid rgba(0, 229, 255, 0.3); background: rgba(0, 229, 255, 0.06); color: #35f8ff; font-size: 11px; font-weight: 600; cursor: pointer; }
.edit-btn svg { width: 12px; height: 12px; }

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

