import request from './request';
import type { ApiResponse } from './dto/base';

export const reviewApi = {
  getReviewWorkbench(taskId: number): Promise<ApiResponse<any>> {
    return request.get(`/review/tasks/${taskId}/view`);
  },
  submitReview(data: any): Promise<ApiResponse<any>> {
    return request.post('/analysis/corrections/review', data);
  }
};
