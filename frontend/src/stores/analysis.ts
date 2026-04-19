import { defineStore } from 'pinia';
import { analysisApi } from '../api/analysis';
import { AnalysisAdapter } from '../api/adapters/analysis';
import type { AnalysisTaskItem, AnalysisDetail } from '../models/analysis';
import type { PaginatedList } from '../models/base';

export const useAnalysisStore = defineStore('analysis', {
  state: () => ({
    tasks: { items: [], total: 0, page: 1, pageSize: 10 } as PaginatedList<AnalysisTaskItem>,
    currentDetail: null as AnalysisDetail | null,
    loading: false,
  }),
  
  actions: {
    async fetchTasks(params: any) {
      this.loading = true;
      try {
        const res = await analysisApi.getTasks(params);
        this.tasks = AnalysisAdapter.toTaskList(res.data);
      } finally {
        this.loading = false;
      }
    },
    
    async fetchDetail(taskId: string | number) {
      this.loading = true;
      try {
        const res = await analysisApi.getTaskDetail(taskId);
        this.currentDetail = AnalysisAdapter.toDetail(res.data);
      } finally {
        this.loading = false;
      }
    }
  }
});
