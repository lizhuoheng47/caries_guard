import request from './request';
import { mockRagApi } from './mock/rag';
import type { RagAnswerDTO } from './dto/rag';
import type { ApiResponse } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export const ragApi = {
  ask(question: string): Promise<ApiResponse<RagAnswerDTO>> {
    if (USE_MOCK) return mockRagApi.ask(question);
    return request.post('/rag/ask', { question });
  }
};
