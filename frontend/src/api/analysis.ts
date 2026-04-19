import request from './request';
import { mockAnalysisApi } from './mock/analysis';
import type { AnalysisTaskListItemDTO, AnalysisDetailViewDTO } from './dto/analysis';
import type { ApiResponse, PageResult } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export const analysisApi = {
  getTasks(params: any): Promise<ApiResponse<PageResult<AnalysisTaskListItemDTO>>> {
    if (USE_MOCK) return mockAnalysisApi.getTasks(params);
    return request.get('/analysis/tasks', { params });
  },

  getTaskDetail(taskId: number): Promise<ApiResponse<AnalysisDetailViewDTO>> {
    if (USE_MOCK) return mockAnalysisApi.getTaskDetail(taskId);
    return request.get(`/analysis/tasks/${taskId}/view`);
  }
};
