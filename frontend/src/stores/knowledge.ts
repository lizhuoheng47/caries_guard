import { defineStore } from 'pinia';
import { knowledgeApi } from '../api/knowledge';
import { KnowledgeAdapter } from '../api/adapters/knowledge';
import type { KnowledgeDocument, KbStats } from '../models/knowledge';
import type { PaginatedList } from '../models/base';

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    stats: null as KbStats | null,
    documents: { items: [], total: 0, page: 1, pageSize: 10 } as PaginatedList<KnowledgeDocument>,
    loading: false,
  }),
  
  actions: {
    async fetchOverview() {
      const res = await knowledgeApi.getOverview();
      this.stats = KnowledgeAdapter.toStats(res.data);
    },
    
    async fetchDocuments(params: any) {
      this.loading = true;
      try {
        const res = await knowledgeApi.getDocuments(params);
        this.documents = KnowledgeAdapter.toDocumentList(res.data);
      } finally {
        this.loading = false;
      }
    }
  }
});
