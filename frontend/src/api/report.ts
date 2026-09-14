import request from './request'
import type { ApiResponse } from './dto/base'

export interface ReportListItem {
  reportId: number
  reportNo: string
  caseId: number
  reportTypeCode: string
  versionNo: number
  reportStatusCode: string
  attachmentId?: number
  generatedAt?: string
  createdAt: string
}

export interface ReportDetail {
  reportId: number
  reportNo: string
  caseId: number
  reportTypeCode: string
  reportStatusCode: string
  versionNo: number
  summaryText?: string
  analysisSummary?: { taskId?: number }
}

export interface ReportExportResult {
  reportId: number
  exported: boolean
  downloadUrl?: string
  expireAt?: number
}

export const reportApi = {
  listReports(limit = 100): Promise<ApiResponse<ReportListItem[]>> {
    return request.get('/reports', { params: { limit } })
  },
  listCaseReports(caseId: number): Promise<ApiResponse<ReportListItem[]>> {
    return request.get(`/cases/${caseId}/reports`)
  },
  getReport(reportId: number): Promise<ApiResponse<ReportDetail>> {
    return request.get(`/reports/${reportId}`)
  },
  generate(caseId: number, doctorConclusion?: string): Promise<ApiResponse<ReportListItem>> {
    return request.post(`/cases/${caseId}/reports`, {
      reportTypeCode: 'DOCTOR',
      doctorConclusion,
      remark: 'Generated from the report center',
    })
  },
  exportPdf(reportId: number): Promise<ApiResponse<ReportExportResult>> {
    return request.post(`/reports/${reportId}/export`, { exportTypeCode: 'PDF', exportChannelCode: 'DOWNLOAD' })
  },
}
