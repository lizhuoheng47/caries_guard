import request from './request'
import { analysisApi } from './analysis'
import type { ApiResponse } from './dto/base'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const buildMockReviewWorkbench = async (taskId: number): Promise<ApiResponse<any>> => {
  const detailRes = await analysisApi.getTaskDetail(taskId)
  const detail = detailRes.data
  const lesions = detail.rawResultJson?.lesionResults || []
  const primaryLesion = lesions[0]
  const secondaryLesion = lesions[1]

  return {
    code: '00000',
    message: 'success',
    data: {
      task: {
        taskId: Number(detail.task.taskId),
        taskNo: detail.task.taskNo,
        createdAt: detail.task.createdAt,
        statusCode: detail.task.taskStatusCode,
      },
      caseInfo: {
        caseId: detail.caseInfo?.caseId || Number(detail.task.caseId),
        caseNo: detail.caseInfo?.caseNo || `CASE-${taskId}`,
        visitTime: detail.caseInfo?.visitTime,
      },
      image: {
        imageId: detail.image?.imageId || Number(taskId) + 800,
        imageUrl: detail.image?.imageUrl || '/mock-xray.jpg',
        sourceDevice: detail.image?.sourceDevice || 'PANORAMIC',
      },
      aiResult: {
        gradingLabel: (detail.analysisSummary?.gradingLabel || 'G2').replace(/^C/, 'G'),
        uncertaintyScore: Number(detail.analysisSummary?.uncertaintyScore ?? 0.22),
        detections: [primaryLesion, secondaryLesion]
          .filter(Boolean)
          .map((lesion: any, index: number) => {
            const [x1 = 0.28 + index * 0.14, y1 = 0.32, x2 = 0.46 + index * 0.14, y2 = 0.54] = lesion?.bbox || []
            return {
              id: lesion?.id || `box-${index + 1}`,
              x: x1 / 512,
              y: y1 / 256,
              width: (x2 - x1) / 512,
              height: (y2 - y1) / 256,
              label: String(lesion?.severityCode || detail.analysisSummary?.gradingLabel || 'C2').replace(/^C/, 'G'),
              confidence: lesion?.confidenceScore,
            }
          }),
      },
      doctorDraft: {
        draftId: Number(taskId) + 9000,
        revisedGrade: (detail.analysisSummary?.gradingLabel || 'G2').replace(/^C/, 'G'),
        revisedDetections: [],
        reasonTags: detail.analysisSummary?.needsReview ? ['边界不清', '建议人工确认'] : [],
        note: detail.rawResultJson?.clinicalSummary || '',
      },
      reviewOptions: {
        gradeOptions: ['G0', 'G1', 'G2', 'G3', 'G4'],
        reasonTags: ['边界不清', '龋损分级调整', '影像质量影响', '需要临床结合', '建议随访观察'],
      },
    },
  }
}

export const reviewApi = {
  getReviewWorkbench(taskId: number): Promise<ApiResponse<any>> {
    if (USE_MOCK) return buildMockReviewWorkbench(taskId)
    return request.get(`/review/tasks/${taskId}/view`)
  },
  submitReview(data: any): Promise<ApiResponse<any>> {
    if (USE_MOCK) {
      return Promise.resolve({
        code: '00000',
        message: 'success',
        data: {
          reviewId: Date.now(),
          taskId: data.taskId,
          revisedGrade: data.revisedGrade,
        },
      })
    }
    return request.post('/analysis/corrections/review', data)
  }
}
