export interface CitationDTO {
  id: string;
  docTitle: string;
  chunkText: string;
  pageNumber?: number;
}

export interface RagAnswerDTO {
  sessionNo: string;
  requestNo: string;
  answer: string;
  answerText: string;
  citations: CitationDTO[];
  retrievedChunks: any[];
  graphEvidence: any[];
  knowledgeBaseCode: string;
  knowledgeVersion: string;
  modelName: string;
  safetyFlag: '0' | '1';
  safetyFlags: string[];
  refusalReason?: string | null;
  confidence?: number | null;
  traceId?: string | null;
  latencyMs: number;
}
