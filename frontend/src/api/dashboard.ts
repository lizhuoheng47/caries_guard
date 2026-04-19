import request from './request';
import type { ApiResponse } from './dto/base';

export const dashboardApi = {
  getNeuralDashboard(): Promise<ApiResponse<any>> {
    return request.get('/dashboard/ai-neural');
  }
};
