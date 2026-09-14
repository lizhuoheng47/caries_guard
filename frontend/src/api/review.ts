import request from './request'
import type { ApiResponse } from './dto/base'

export const reviewApi = {
  getReviewQueue(params: { pageNo?: number; pageSize?: number } = {}): Promise<ApiResponse<any>> {
    return request.get('/review/queue', { params })
  },
  getReviewWorkbench(taskId: number): Promise<ApiResponse<any>> {
    return request.get(`/review/tasks/${taskId}/view`)
  },
  saveDraft(taskId: number, data: any): Promise<ApiResponse<any>> {
    return request.put(`/review/tasks/${taskId}/draft`, data)
  },
  submitReview(taskId: number, data: any): Promise<ApiResponse<any>> {
    return request.post(`/review/tasks/${taskId}/submit`, data)
  },
}
