import { defineStore } from 'pinia';
import { ragApi } from '../api/rag';
import { RagAdapter } from '../api/adapters/rag';
import type { RagResponse } from '../models/rag';

export const useRagStore = defineStore('rag', {
  state: () => ({
    history: [] as { role: 'user'|'ai', content: string, response?: RagResponse }[],
    loading: false,
  }),
  
  actions: {
    async askQuestion(question: string) {
      this.history.push({ role: 'user', content: question });
      this.loading = true;
      
      try {
        const res = await ragApi.ask(question);
        const ragRes = RagAdapter.toResponse(res.data);
        this.history.push({ role: 'ai', content: ragRes.text, response: ragRes });
      } catch (error) {
        this.history.push({ role: 'ai', content: 'An error occurred during knowledge retrieval.' });
      } finally {
        this.loading = false;
      }
    },
    
    clearHistory() {
      this.history = [];
    }
  }
});
