<template>
  <div class="med-page settings-page">
    <section class="med-hero">
      <div class="med-hero-main">
        <div class="med-eyebrow">System Preferences</div>
        <h1 class="med-title">系统设置</h1>
        <p class="med-subtitle">
          管理当前工作站的刷新频率、复核习惯、报告输出偏好与告警阈值。设置会保存到浏览器本地，并即时影响队列、详情、报告等页面。
        </p>
      </div>
      <div class="med-action-row">
        <button class="med-btn med-btn--ghost" @click="resetAll">
          <AppIcon name="contrast" :size="14" />
          恢复默认
        </button>
        <button class="med-btn med-btn--primary" @click="saveAll">
          <AppIcon name="check" :size="14" />
          保存设置
        </button>
      </div>
    </section>

    <section class="med-metric-grid">
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Auto Refresh</div>
        <div class="med-metric-value">{{ form.autoRefreshSeconds }}s</div>
        <div class="med-metric-caption">任务队列自动同步周期</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Confidence Gate</div>
        <div class="med-metric-value">{{ Math.round(form.confidenceThreshold * 100) }}%</div>
        <div class="med-metric-caption">低于该值时提示人工确认</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Risk Alert</div>
        <div class="med-metric-value">{{ Math.round(form.riskAlertThreshold * 100) }}%</div>
        <div class="med-metric-caption">高于该值时标记高关注</div>
      </article>
      <article class="med-card med-metric-card">
        <div class="med-metric-label">Workspace Owner</div>
        <div class="med-metric-value owner-name">{{ authStore.user?.nickname || authStore.user?.username || 'Doctor' }}</div>
        <div class="med-metric-caption">当前登录账号的工作台偏好</div>
      </article>
    </section>

    <section class="med-grid-2 settings-grid">
      <article class="med-card">
        <div class="med-card-inner settings-section">
          <div class="med-section-head">
            <h2 class="med-section-title">工作流偏好</h2>
          </div>

          <div class="med-input-wrap">
            <span class="queue-label">默认入口</span>
            <div class="med-tabs">
              <button v-for="item in landingOptions" :key="item.value" class="med-tab" :class="{ 'is-active': form.defaultLanding === item.value }" @click="form.defaultLanding = item.value">
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="med-input-wrap">
            <span class="queue-label">详情页默认图层</span>
            <div class="med-tabs">
              <button v-for="item in imageModeOptions" :key="item.value" class="med-tab" :class="{ 'is-active': form.defaultImageMode === item.value }" @click="form.defaultImageMode = item.value">
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="med-input-wrap">
            <span class="queue-label">自动刷新间隔</span>
            <input v-model.number="form.autoRefreshSeconds" class="med-range" type="range" min="0" max="120" step="5" />
            <div class="settings-range-meta">
              <span class="med-meta">0 秒表示关闭自动刷新</span>
              <span class="med-mono">{{ form.autoRefreshSeconds }} s</span>
            </div>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">自动打开最新复核任务</div>
              <div class="med-meta">进入复核页但未指定任务时，自动选中最新一条</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.autoOpenNewestReview }" @click="form.autoOpenNewestReview = !form.autoOpenNewestReview"></button>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">紧凑队列模式</div>
              <div class="med-meta">用于后续进一步压缩卡片信息密度</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.queueCompactMode }" @click="form.queueCompactMode = !form.queueCompactMode"></button>
          </div>
        </div>
      </article>

      <article class="med-card">
        <div class="med-card-inner settings-section">
          <div class="med-section-head">
            <h2 class="med-section-title">告警阈值</h2>
          </div>

          <div class="med-input-wrap">
            <span class="queue-label">置信度下限</span>
            <input v-model.number="form.confidenceThreshold" class="med-range" type="range" min="0.5" max="0.99" step="0.01" />
            <div class="settings-range-meta">
              <span class="med-meta">分析详情页将以此提醒是否需要人工确认</span>
              <span class="med-mono">{{ Math.round(form.confidenceThreshold * 100) }}%</span>
            </div>
          </div>

          <div class="med-input-wrap">
            <span class="queue-label">不确定性告警值</span>
            <input v-model.number="form.riskAlertThreshold" class="med-range" type="range" min="0.05" max="0.95" step="0.01" />
            <div class="settings-range-meta">
              <span class="med-meta">高于该值的任务将在列表中显示更高风险颜色</span>
              <span class="med-mono">{{ Math.round(form.riskAlertThreshold * 100) }}%</span>
            </div>
          </div>

          <div class="med-note">
            阈值仅影响前端工作台提示，不直接修改后端模型输出。如果要做正式业务规则调整，建议在服务端同步设置。
          </div>
        </div>
      </article>

      <article class="med-card">
        <div class="med-card-inner settings-section">
          <div class="med-section-head">
            <h2 class="med-section-title">报告偏好</h2>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">默认包含治疗计划</div>
              <div class="med-meta">报告页初始打开时默认展示治疗建议区块</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.reportIncludeTreatmentPlan }" @click="form.reportIncludeTreatmentPlan = !form.reportIncludeTreatmentPlan"></button>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">默认包含证据引用</div>
              <div class="med-meta">报告页初始打开时展示 RAG / 证据来源内容</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.reportIncludeCitations }" @click="form.reportIncludeCitations = !form.reportIncludeCitations"></button>
          </div>

          <div class="med-note">
            当前报告页支持在页面顶部临时切换显示项，保存设置后则会作为默认值生效。
          </div>
        </div>
      </article>

      <article class="med-card">
        <div class="med-card-inner settings-section">
          <div class="med-section-head">
            <h2 class="med-section-title">通知策略</h2>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">任务完成提醒</div>
              <div class="med-meta">为后续实时通知预留开关</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.notifyTaskFinished }" @click="form.notifyTaskFinished = !form.notifyTaskFinished"></button>
          </div>

          <div class="med-toggle-row">
            <div>
              <div class="settings-toggle-title">复核到达提醒</div>
              <div class="med-meta">高优先级任务进入复核时，可用于前端提醒策略</div>
            </div>
            <button class="med-switch" :class="{ 'is-on': form.notifyReviewReady }" @click="form.notifyReviewReady = !form.notifyReviewReady"></button>
          </div>

          <div class="med-note">
            这些开关当前会保存并广播到工作台，后续可继续接入 WebSocket / SSE 事件流。
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import {
  defaultWorkspaceSettings,
  loadWorkspaceSettings,
  resetWorkspaceSettings,
  saveWorkspaceSettings,
  type WorkspaceImageMode,
  type WorkspaceLanding,
  type WorkspaceSettings,
} from '@/utils/workbenchSettings'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const form = reactive<WorkspaceSettings>({ ...loadWorkspaceSettings() })

const landingOptions: Array<{ value: WorkspaceLanding; label: string }> = [
  { value: 'dashboard', label: '工作台' },
  { value: 'cases', label: '病例中心' },
  { value: 'analysis', label: '分析队列' },
  { value: 'ai-diagnosis', label: 'AI 诊断' },
]

const imageModeOptions: Array<{ value: WorkspaceImageMode; label: string }> = [
  { value: 'overlay', label: 'Overlay' },
  { value: 'heatmap', label: 'Heatmap' },
  { value: 'original', label: 'Original' },
]

const syncForm = (settings: WorkspaceSettings) => {
  Object.assign(form, settings)
}

const saveAll = () => {
  const saved = saveWorkspaceSettings({ ...form })
  syncForm(saved)
  notificationStore.success('设置已保存', '新的工作台偏好已即时生效。')
}

const resetAll = () => {
  const saved = resetWorkspaceSettings()
  syncForm(saved)
  notificationStore.info('已恢复默认', '工作台设置已重置为推荐默认值。')
}

if (!form.defaultLanding) {
  syncForm({ ...defaultWorkspaceSettings })
}
</script>

<style scoped>
.settings-page {
  gap: 16px;
}

.settings-grid {
  align-items: start;
}

.settings-section {
  display: grid;
  gap: 18px;
}

.settings-toggle-title {
  color: var(--text);
  font-size: 14px;
  font-weight: 700;
}

.settings-range-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.owner-name {
  font-size: 22px;
}

.queue-label {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-dim);
}
</style>
