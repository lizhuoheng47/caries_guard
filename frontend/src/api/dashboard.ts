import request from './request';
import type { ApiResponse } from './dto/base';

export interface DashboardOverview {
  patientCount: number
  caseCount: number
  analyzedCaseCount: number
  generatedReportCount: number
  followupRequiredCaseCount: number
  closedCaseCount: number
  todayAnalysisTaskCount: number
  averageInferenceMillis?: number
  highUncertaintyRate?: number
  reviewPassRate?: number
  doctorAdoptionRate?: number
}

export interface DashboardTrendPoint {
  date: string
  newCaseCount: number
  analysisCompletedCount: number
  reportGeneratedCount: number
  followupTriggeredCount: number
}

export interface RiskDistribution {
  highRiskCount: number
  mediumRiskCount: number
  lowRiskCount: number
  totalCount: number
}

export const dashboardApi = {
  getOverview(): Promise<ApiResponse<DashboardOverview>> {
    return request.get('/dashboard/overview')
  },
  getTrend(rangeType: 'LAST_7_DAYS' | 'LAST_30_DAYS'): Promise<ApiResponse<DashboardTrendPoint[]>> {
    return request.get('/dashboard/trend', { params: { rangeType } })
  },
  getRiskDistribution(): Promise<ApiResponse<RiskDistribution>> {
    return request.get('/dashboard/risk-level-distribution')
  },
};
