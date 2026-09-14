import request from './request';
import type { AnalysisDetailViewDTO, AnalysisTaskPageDTO } from './dto/analysis';
import type { ApiResponse } from './dto/base';

export const analysisApi = {
  getTasks(params: any): Promise<ApiResponse<AnalysisTaskPageDTO>> {
    return request.get('/analysis/tasks', { params });
  },

  getTaskDetail(taskId: string | number): Promise<ApiResponse<AnalysisDetailViewDTO>> {
    return request.get(`/analysis/tasks/${taskId}/view`);
  }
};
