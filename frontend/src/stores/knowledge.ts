import { defineStore } from 'pinia';
import { knowledgeApi, type KnowledgeDocumentsQuery, type KnowledgeVersionActionPayload } from '../api/knowledge';
import { KnowledgeAdapter } from '../api/adapters/knowledge';
import type { PaginatedList } from '../models/base';
import type { KnowledgeDocument, KnowledgeDocumentDetail, KbStats } from '../models/knowledge';

const defaultListState = (): PaginatedList<KnowledgeDocument> => ({
  items: [],
  total: 0,
  page: 1,
  pageSize: 10,
});

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    stats: null as KbStats | null,
    documents: defaultListState(),
    loading: false,
    uploading: false,
    rebuilding: false,
    docBusyMap: {} as Record<number, boolean>,
  }),

  getters: {
    isDocBusy: (state) => (docId: number) => Boolean(state.docBusyMap[docId]),
  },

  actions: {
    async fetchOverview() {
      const res = await knowledgeApi.getOverview();
      this.stats = KnowledgeAdapter.toStats(res.data);
    },

    async fetchDocuments(params: KnowledgeDocumentsQuery = {}) {
      this.loading = true;
      try {
        const resolvedParams: KnowledgeDocumentsQuery = {
          pageNum: params.pageNum ?? this.documents.page ?? 1,
          pageSize: params.pageSize ?? this.documents.pageSize ?? 10,
          keyword: params.keyword,
          kbCode: params.kbCode,
        };
        const res = await knowledgeApi.getDocuments(resolvedParams);
        this.documents = KnowledgeAdapter.toDocumentList(res.data, resolvedParams);
      } finally {
        this.loading = false;
      }
    },

    async fetchDocumentDetail(docId: number): Promise<KnowledgeDocumentDetail> {
      const res = await knowledgeApi.getDocumentDetail(docId);
      return KnowledgeAdapter.toDocumentDetail(res.data);
    },

    async uploadDocument(file: File) {
      this.uploading = true;
      try {
        await knowledgeApi.uploadDocument({
          file,
          docTitle: file.name.replace(/\.[^.]+$/, ''),
          docSourceCode: 'UPLOAD',
        });
      } finally {
        this.uploading = false;
      }
    },

    async rebuildIndex() {
      this.rebuilding = true;
      try {
        await knowledgeApi.rebuild({});
      } finally {
        this.rebuilding = false;
      }
    },

    async submitReview(docId: number, payload: KnowledgeVersionActionPayload) {
      await this.withDocBusy(docId, () => knowledgeApi.submitReview(docId, payload));
    },

    async approve(docId: number, payload: KnowledgeVersionActionPayload) {
      await this.withDocBusy(docId, () => knowledgeApi.approve(docId, payload));
    },

    async reject(docId: number, payload: KnowledgeVersionActionPayload) {
      await this.withDocBusy(docId, () => knowledgeApi.reject(docId, payload));
    },

    async publish(docId: number, payload: KnowledgeVersionActionPayload) {
      await this.withDocBusy(docId, () => knowledgeApi.publish(docId, payload));
    },

    async rollback(docId: number, payload: KnowledgeVersionActionPayload) {
      await this.withDocBusy(docId, () => knowledgeApi.rollback(docId, payload));
    },

    async reindexDocument(doc: KnowledgeDocument) {
      const versionNo = doc.publishedVersionNo ?? doc.currentVersionNo ?? doc.version;
      if (!versionNo) {
        throw new Error('Missing document version');
      }
      await this.publish(doc.id, { versionNo, comment: 'Manual reindex from knowledge page' });
    },

    async deleteDocument(docId: number) {
      await this.withDocBusy(docId, () => knowledgeApi.deleteDocument(docId));
    },

    async withDocBusy<T>(docId: number, fn: () => Promise<T>): Promise<T> {
      this.docBusyMap = { ...this.docBusyMap, [docId]: true };
      try {
        return await fn();
      } finally {
        const next = { ...this.docBusyMap };
        delete next[docId];
        this.docBusyMap = next;
      }
    },
  },
});
