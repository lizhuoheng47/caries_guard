import request from './request';
import { mockKnowledgeApi } from './mock/knowledge';
import type { KnowledgeDocumentListItemDTO, KbOverviewDTO } from './dto/knowledge';
import type { ApiResponse, PageResult } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export const knowledgeApi = {
  getOverview(): Promise<ApiResponse<KbOverviewDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.getOverview();
    return request.get('/kb/overview');
  },

  getDocuments(params: any): Promise<ApiResponse<PageResult<KnowledgeDocumentListItemDTO>>> {
    if (USE_MOCK) return mockKnowledgeApi.getDocuments(params);
    return request.get('/kb/documents', { params });
  }
};
