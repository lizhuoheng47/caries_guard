import type { RagAnswerDTO } from '../dto/rag';
import type { RagResponse } from '../../models/rag';

export const RagAdapter = {
  toResponse(dto: RagAnswerDTO): RagResponse {
    return {
      session: dto.sessionNo,
      request: dto.requestNo,
      text: dto.answerText || dto.answer,
      citations: dto.citations.map(c => ({
        id: c.id,
        title: c.docTitle,
        text: c.chunkText,
        page: c.pageNumber
      })),
      version: dto.knowledgeVersion,
      safetyWarning: dto.safetyFlag === '1',
      safetyMessages: dto.safetyFlags,
      confidence: dto.confidence || undefined,
    };
  }
};
