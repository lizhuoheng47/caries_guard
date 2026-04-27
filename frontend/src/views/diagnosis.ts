export type DiagState = 'empty' | 'scanning' | 'result'

export type RiskLevel = 'high' | 'medium' | 'low' | 'info'

export interface Annotation {
  id: number
  x: number
  y: number
  color: string
  label: string
}

export interface Diagnosis {
  level: RiskLevel
  text: string
}

export interface AnalysisResults {
  teethDetected: number
  anomalies: number
  severity: string
  confidence: number
  overallScore: number
  riskHigh: number
  riskMedium: number
  riskLow: number
  diagnoses: Diagnosis[]
}

export const RISK_COLORS: Record<RiskLevel, string> = {
  high: '#FF4757',
  medium: '#FF9F43',
  low: '#FFC312',
  info: '#2CFAB5',
}

export const MOCK_RESULTS: AnalysisResults = {
  teethDetected: 28,
  anomalies: 7,
  severity: '中度',
  confidence: 92.7,
  overallScore: 85,
  riskHigh: 2,
  riskMedium: 3,
  riskLow: 2,
  diagnoses: [
    { level: 'high', text: '右下阻生智齿位置异常，建议进一步评估拔除时机。' },
    { level: 'high', text: '左上第二前磨牙疑似龋坏，建议尽快补牙处理。' },
    { level: 'medium', text: '右下第一磨牙存在中度牙周炎征象，建议进行牙周治疗。' },
    { level: 'info', text: '建议定期复查并持续维护口腔卫生习惯。' },
  ],
}

export const MOCK_ANNOTATIONS: Annotation[] = [
  { id: 1, x: 25, y: 55, color: '#9B59B6', label: '左上第二前磨牙龋坏' },
  { id: 2, x: 35, y: 58, color: '#3498DB', label: '牙周组织正常' },
  { id: 3, x: 45, y: 60, color: '#2CFAB5', label: '牙体完整' },
  { id: 4, x: 60, y: 58, color: '#FF9F43', label: '轻度磨损' },
  { id: 5, x: 70, y: 55, color: '#FF9F43', label: '中度牙周炎' },
  { id: 6, x: 78, y: 53, color: '#FF4757', label: '阻生智齿' },
  { id: 7, x: 82, y: 56, color: '#FF4757', label: '建议拔除' },
]
