import request from './request';
import { mockKnowledgeApi } from './mock/knowledge';
import type { ApiResponse } from './dto/base';
import type {
  KnowledgeActionResultDTO,
  KnowledgeDocumentDetailDTO,
  KnowledgeDocumentsResultDTO,
  KnowledgeRebuildResultDTO,
  KnowledgeUploadResultDTO,
  KbOverviewDTO,
} from './dto/knowledge';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export interface KnowledgeDocumentsQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  kbCode?: string;
}

export interface KnowledgeUploadPayload {
  file: File;
  kbCode?: string;
  kbName?: string;
  kbTypeCode?: string;
  docTitle?: string;
  docSourceCode?: string;
  sourceUri?: string;
  docNo?: string;
  docVersion?: string;
  changeSummary?: string;
}

export interface KnowledgeVersionActionPayload {
  versionNo: string;
  comment?: string;
}

export interface KnowledgeRebuildPayload {
  kbCode?: string;
  kbName?: string;
  kbTypeCode?: string;
  knowledgeVersion?: string;
  rebuildParse?: boolean;
  rebuildLexical?: boolean;
  rebuildDense?: boolean;
  rebuildGraph?: boolean;
  cleanupStale?: boolean;
}

const sendVersionAction = (
  docId: number,
  action: 'submit-review' | 'approve' | 'reject' | 'publish' | 'rollback',
  payload: KnowledgeVersionActionPayload,
) => request.post(`/kb/documents/${docId}/${action}`, payload) as Promise<ApiResponse<KnowledgeActionResultDTO>>;

export const knowledgeApi = {
  getOverview(): Promise<ApiResponse<KbOverviewDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.getOverview();
    return request.get('/kb/overview');
  },

  getDocuments(params: KnowledgeDocumentsQuery = {}): Promise<ApiResponse<KnowledgeDocumentsResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.getDocuments(params);
    return request.get('/kb/documents', { params });
  },

  getDocumentDetail(docId: number): Promise<ApiResponse<KnowledgeDocumentDetailDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.getDocumentDetail(docId);
    return request.get(`/kb/documents/${docId}`);
  },

  uploadDocument(payload: KnowledgeUploadPayload): Promise<ApiResponse<KnowledgeUploadResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.uploadDocument(payload);

    const formData = new FormData();
    formData.append('file', payload.file);

    if (payload.kbCode) formData.append('kbCode', payload.kbCode);
    if (payload.kbName) formData.append('kbName', payload.kbName);
    if (payload.kbTypeCode) formData.append('kbTypeCode', payload.kbTypeCode);
    if (payload.docTitle) formData.append('docTitle', payload.docTitle);
    if (payload.docSourceCode) formData.append('docSourceCode', payload.docSourceCode);
    if (payload.sourceUri) formData.append('sourceUri', payload.sourceUri);
    if (payload.docNo) formData.append('docNo', payload.docNo);
    if (payload.docVersion) formData.append('docVersion', payload.docVersion);
    if (payload.changeSummary) formData.append('changeSummary', payload.changeSummary);

    return request.post('/kb/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  rebuild(payload: KnowledgeRebuildPayload): Promise<ApiResponse<KnowledgeRebuildResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.rebuild(payload);
    return request.post('/kb/rebuild', payload);
  },

  submitReview(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.submitReview(docId, payload);
    return sendVersionAction(docId, 'submit-review', payload);
  },

  approve(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.approve(docId, payload);
    return sendVersionAction(docId, 'approve', payload);
  },

  reject(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.reject(docId, payload);
    return sendVersionAction(docId, 'reject', payload);
  },

  publish(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.publish(docId, payload);
    return sendVersionAction(docId, 'publish', payload);
  },

  rollback(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.rollback(docId, payload);
    return sendVersionAction(docId, 'rollback', payload);
  },

  deleteDocument(docId: number): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    if (USE_MOCK) return mockKnowledgeApi.deleteDocument(docId);
    return request.delete(`/kb/documents/${docId}`);
  },
};
