import type { ApiResponse } from '../dto/base';
import type {
  KnowledgeActionResultDTO,
  KnowledgeDocumentDetailDTO,
  KnowledgeDocumentListItemDTO,
  KnowledgeDocumentsResultDTO,
  KnowledgeRebuildResultDTO,
  KnowledgeUploadResultDTO,
  KbOverviewDTO,
} from '../dto/knowledge';
import type {
  KnowledgeRebuildPayload,
  KnowledgeUploadPayload,
  KnowledgeVersionActionPayload,
} from '../knowledge';

let docs: KnowledgeDocumentListItemDTO[] = [
  {
    docId: 1,
    docNo: 'DOC-2026-001',
    docTitle: 'ICDAS Clinical Guide v2',
    docSourceCode: 'MANUAL',
    reviewStatusCode: 'APPROVED',
    publishStatusCode: 'PUBLISHED',
    currentVersionNo: 'v2.1',
    publishedVersionNo: 'v2.1',
    chunkCount: 1240,
    entityCount: 450,
    relationCount: 890,
    updatedAt: new Date().toISOString(),
  },
  {
    docId: 2,
    docNo: 'DOC-2026-002',
    docTitle: 'Operative Dentistry Protocols',
    docSourceCode: 'GUIDELINE',
    reviewStatusCode: 'REVIEW_PENDING',
    publishStatusCode: 'DRAFT',
    currentVersionNo: 'v1.3',
    chunkCount: 850,
    entityCount: 320,
    relationCount: 610,
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
  },
];

const success = <T>(data: T): Promise<ApiResponse<T>> =>
  Promise.resolve({
    code: '00000',
    message: 'success',
    data,
  });

const patchDoc = (docId: number, patch: Partial<KnowledgeDocumentListItemDTO>) => {
  docs = docs.map((item) => (item.docId === docId ? { ...item, ...patch, updatedAt: new Date().toISOString() } : item));
};

export const mockKnowledgeApi = {
  getOverview(): Promise<ApiResponse<KbOverviewDTO>> {
    const chunkCount = docs.reduce((sum, doc) => sum + (doc.chunkCount ?? 0), 0);
    return success({
      documentCount: docs.length,
      chunkCount,
      currentVersion: 'v2.1',
      lastIndexedAt: new Date().toISOString(),
    });
  },

  getDocuments(params: { pageNum?: number; pageSize?: number }): Promise<ApiResponse<KnowledgeDocumentsResultDTO>> {
    const pageNum = params.pageNum && params.pageNum > 0 ? params.pageNum : 1;
    const pageSize = params.pageSize && params.pageSize > 0 ? params.pageSize : 10;
    const start = (pageNum - 1) * pageSize;
    const list = docs.slice(start, start + pageSize);

    return success({
      list,
      total: docs.length,
      pageNum,
      pageSize,
    });
  },

  getDocumentDetail(docId: number): Promise<ApiResponse<KnowledgeDocumentDetailDTO>> {
    const found = docs.find((item) => item.docId === docId);
    if (!found) {
      return success({
        docId,
        docNo: `DOC-${docId}`,
        docTitle: 'Unknown Document',
        docSourceCode: 'UNKNOWN',
        reviewStatusCode: 'PENDING',
        publishStatusCode: 'DRAFT',
        currentVersionNo: 'v1.0',
        updatedAt: new Date().toISOString(),
        versions: [],
        reviewRecords: [],
        publishRecords: [],
      });
    }

    return success({
      ...found,
      contentText: '# Mock Content\nThis is a mock knowledge document body.',
      versions: [
        {
          versionNo: found.currentVersionNo ?? 'v1.0',
          reviewStatusCode: found.reviewStatusCode,
          publishStatusCode: found.publishStatusCode,
          createdAt: found.updatedAt,
          updatedAt: found.updatedAt,
        },
      ],
      reviewRecords: [],
      publishRecords: [],
    });
  },

  uploadDocument(payload: KnowledgeUploadPayload): Promise<ApiResponse<KnowledgeUploadResultDTO>> {
    const docId = (docs[0]?.docId ?? 0) + 1;
    const docNo = payload.docNo ?? `DOC-2026-${docId.toString().padStart(3, '0')}`;
    const docTitle = payload.docTitle ?? payload.file.name.replace(/\.[^.]+$/, '');
    const versionNo = payload.docVersion ?? 'v1.0';

    docs = [
      {
        docId,
        docNo,
        docTitle,
        docSourceCode: payload.docSourceCode ?? 'UPLOAD',
        reviewStatusCode: 'REVIEW_PENDING',
        publishStatusCode: 'DRAFT',
        currentVersionNo: versionNo,
        chunkCount: 0,
        entityCount: 0,
        relationCount: 0,
        updatedAt: new Date().toISOString(),
      },
      ...docs,
    ];

    return success({
      docId,
      docNo,
      docTitle,
      versionNo,
      reviewStatusCode: 'REVIEW_PENDING',
      publishStatusCode: 'DRAFT',
      chunkCount: 0,
      entityCount: 0,
      relationCount: 0,
      ingestJobNo: `INGEST-${Date.now()}`,
    });
  },

  rebuild(_payload: KnowledgeRebuildPayload): Promise<ApiResponse<KnowledgeRebuildResultDTO>> {
    return success({
      rebuildJobNo: `KBREBUILD-${Date.now()}`,
      rebuildStatusCode: 'SUCCESS',
      chunkCount: docs.reduce((sum, doc) => sum + (doc.chunkCount ?? 0), 0),
    });
  },

  submitReview(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    patchDoc(docId, { reviewStatusCode: 'REVIEW_PENDING', currentVersionNo: payload.versionNo });
    return success({ docId, versionNo: payload.versionNo, status: 'REVIEW_PENDING' });
  },

  approve(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    patchDoc(docId, { reviewStatusCode: 'APPROVED', currentVersionNo: payload.versionNo });
    return success({ docId, versionNo: payload.versionNo, status: 'APPROVED' });
  },

  reject(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    patchDoc(docId, { reviewStatusCode: 'REJECTED', currentVersionNo: payload.versionNo });
    return success({ docId, versionNo: payload.versionNo, status: 'REJECTED' });
  },

  publish(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    patchDoc(docId, {
      publishStatusCode: 'PUBLISHED',
      reviewStatusCode: 'APPROVED',
      currentVersionNo: payload.versionNo,
      publishedVersionNo: payload.versionNo,
    });
    return success({ docId, versionNo: payload.versionNo, status: 'PUBLISHED' });
  },

  rollback(docId: number, payload: KnowledgeVersionActionPayload): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    patchDoc(docId, { currentVersionNo: payload.versionNo, publishedVersionNo: payload.versionNo });
    return success({ docId, versionNo: payload.versionNo, status: 'ROLLED_BACK' });
  },

  deleteDocument(docId: number): Promise<ApiResponse<KnowledgeActionResultDTO>> {
    docs = docs.filter((item) => item.docId !== docId);
    return success({ docId, status: 'DELETED' });
  },
};
