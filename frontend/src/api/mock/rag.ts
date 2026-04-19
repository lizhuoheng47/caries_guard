import type { ApiResponse } from '../dto/base';
import type { RagAnswerDTO } from '../dto/rag';

export const mockRagApi = {
  ask(question: string): Promise<ApiResponse<RagAnswerDTO>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: '00000',
          message: 'success',
          data: {
            sessionNo: 'SESS-123',
            requestNo: 'REQ-456',
            answer: `Based on the provided guidelines, a G3 caries involves the inner half of the dentin. The AI detected this with 0.85 confidence. [1] Treatment requires immediate operative intervention. [2]`,
            answerText: `Based on the provided guidelines, a G3 caries involves the inner half of the dentin. The AI detected this with 0.85 confidence. Treatment requires immediate operative intervention.`,
            citations: [
              { id: '1', docTitle: 'ICDAS Clinical Guide v2', chunkText: 'G3 is defined as distinct visual change in enamel with localized enamel breakdown or underlying dentin shadow...', pageNumber: 12 },
              { id: '2', docTitle: 'Operative Dentistry Protocols', chunkText: 'For G3 lesions, immediate excavation and restoration is recommended to prevent pulpitis.', pageNumber: 45 }
            ],
            retrievedChunks: [],
            graphEvidence: [],
            knowledgeBaseCode: 'KB-CARIES-2026',
            knowledgeVersion: 'v2.1.0',
            modelName: 'gemini-1.5-pro',
            safetyFlag: '1',
            safetyFlags: ['AI suggestion is for reference only, please follow medical protocols.'],
            confidence: 0.92,
            latencyMs: 1450,
          }
        });
      }, 1000);
    });
  }
};
