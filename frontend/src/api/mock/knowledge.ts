import type { ApiResponse, PageResult } from '../dto/base';
import type { KnowledgeDocumentListItemDTO, KbOverviewDTO } from '../dto/knowledge';

export const mockKnowledgeApi = {
  getOverview(): Promise<ApiResponse<KbOverviewDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        totalDocs: 42,
        totalChunks: 15420,
        lastIndexedAt: new Date().toISOString(),
        currentVersion: 'v2.1.0'
      }
    });
  },

  getDocuments(params: any): Promise<ApiResponse<PageResult<KnowledgeDocumentListItemDTO>>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        total: 42,
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 10,
        list: [
          {
            docId: 1,
            docNo: 'DOC-2026-001',
            docTitle: 'ICDAS Clinical Guide v2',
            docSourceCode: 'MANUAL',
            reviewStatusCode: 'APPROVED',
            publishStatusCode: 'PUBLISHED',
            currentVersionNo: 'v2',
            publishedVersionNo: 'v2',
            chunkCount: 1240,
            entityCount: 450,
            relationCount: 890,
            updatedAt: new Date().toISOString()
          },
          {
            docId: 2,
            docNo: 'DOC-2026-002',
            docTitle: 'Operative Dentistry Protocols',
            docSourceCode: 'GUIDELINE',
            reviewStatusCode: 'PENDING',
            publishStatusCode: 'UNPUBLISHED',
            currentVersionNo: 'v1',
            chunkCount: 850,
            entityCount: 320,
            relationCount: 610,
            updatedAt: new Date(Date.now() - 86400000).toISOString()
          }
        ]
      }
    });
  }
};
