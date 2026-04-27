export type WorkspaceLanding = 'dashboard' | 'cases' | 'analysis' | 'ai-diagnosis'
export type WorkspaceImageMode = 'overlay' | 'heatmap' | 'original'

export interface WorkspaceSettings {
  autoRefreshSeconds: number
  queueCompactMode: boolean
  defaultLanding: WorkspaceLanding
  confidenceThreshold: number
  riskAlertThreshold: number
  reportIncludeCitations: boolean
  reportIncludeTreatmentPlan: boolean
  notifyTaskFinished: boolean
  notifyReviewReady: boolean
  autoOpenNewestReview: boolean
  defaultImageMode: WorkspaceImageMode
}

const STORAGE_KEY = 'dentai.workspace.settings'
export const WORKSPACE_SETTINGS_EVENT = 'dentai:workspace-settings-updated'

export const defaultWorkspaceSettings: WorkspaceSettings = {
  autoRefreshSeconds: 30,
  queueCompactMode: false,
  defaultLanding: 'dashboard',
  confidenceThreshold: 0.8,
  riskAlertThreshold: 0.35,
  reportIncludeCitations: true,
  reportIncludeTreatmentPlan: true,
  notifyTaskFinished: true,
  notifyReviewReady: true,
  autoOpenNewestReview: true,
  defaultImageMode: 'overlay',
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const normalizeSettings = (input?: Partial<WorkspaceSettings> | null): WorkspaceSettings => ({
  autoRefreshSeconds: clamp(Number(input?.autoRefreshSeconds ?? defaultWorkspaceSettings.autoRefreshSeconds) || 0, 0, 300),
  queueCompactMode: Boolean(input?.queueCompactMode),
  defaultLanding: ['dashboard', 'cases', 'analysis', 'ai-diagnosis'].includes(String(input?.defaultLanding))
    ? (input?.defaultLanding as WorkspaceLanding)
    : defaultWorkspaceSettings.defaultLanding,
  confidenceThreshold: clamp(Number(input?.confidenceThreshold ?? defaultWorkspaceSettings.confidenceThreshold) || 0, 0.5, 0.99),
  riskAlertThreshold: clamp(Number(input?.riskAlertThreshold ?? defaultWorkspaceSettings.riskAlertThreshold) || 0, 0.05, 0.95),
  reportIncludeCitations: input?.reportIncludeCitations ?? defaultWorkspaceSettings.reportIncludeCitations,
  reportIncludeTreatmentPlan: input?.reportIncludeTreatmentPlan ?? defaultWorkspaceSettings.reportIncludeTreatmentPlan,
  notifyTaskFinished: input?.notifyTaskFinished ?? defaultWorkspaceSettings.notifyTaskFinished,
  notifyReviewReady: input?.notifyReviewReady ?? defaultWorkspaceSettings.notifyReviewReady,
  autoOpenNewestReview: input?.autoOpenNewestReview ?? defaultWorkspaceSettings.autoOpenNewestReview,
  defaultImageMode: ['overlay', 'heatmap', 'original'].includes(String(input?.defaultImageMode))
    ? (input?.defaultImageMode as WorkspaceImageMode)
    : defaultWorkspaceSettings.defaultImageMode,
})

const emitSettingsEvent = (settings: WorkspaceSettings) => {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(WORKSPACE_SETTINGS_EVENT, { detail: settings }))
}

export const loadWorkspaceSettings = (): WorkspaceSettings => {
  if (typeof window === 'undefined') return { ...defaultWorkspaceSettings }

  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...defaultWorkspaceSettings }
    return normalizeSettings(JSON.parse(raw) as Partial<WorkspaceSettings>)
  } catch {
    return { ...defaultWorkspaceSettings }
  }
}

export const saveWorkspaceSettings = (settings: WorkspaceSettings) => {
  const normalized = normalizeSettings(settings)
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
    emitSettingsEvent(normalized)
  }
  return normalized
}

export const updateWorkspaceSettings = (patch: Partial<WorkspaceSettings>) => {
  return saveWorkspaceSettings({ ...loadWorkspaceSettings(), ...patch })
}

export const resetWorkspaceSettings = () => saveWorkspaceSettings({ ...defaultWorkspaceSettings })

export const onWorkspaceSettingsChange = (handler: (settings: WorkspaceSettings) => void) => {
  if (typeof window === 'undefined') return () => undefined
  const listener = (event: Event) => {
    const customEvent = event as CustomEvent<WorkspaceSettings>
    if (customEvent.detail) handler(customEvent.detail)
  }
  window.addEventListener(WORKSPACE_SETTINGS_EVENT, listener)
  return () => window.removeEventListener(WORKSPACE_SETTINGS_EVENT, listener)
}
