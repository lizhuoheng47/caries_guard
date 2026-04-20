export interface CitationDTO {
  id: string;
  docTitle: string;
  chunkText: string;
  pageNumber?: number;
  sourceUri?: string;
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
  caseContextSummary?: string | null;
  confidence?: number | null;
  traceId?: string | null;
  latencyMs: number;
  fallback?: boolean;
}

export interface RagAskCommandDTO {
  question: string;
  scene?: string;
  kbCode?: string;
  topK?: number;
  relatedBizNo?: string;
  patientUuid?: string;
  caseContext?: Record<string, unknown>;
}

export interface DoctorQaCommandDTO {
  question: string;
  kbCode?: string;
  topK?: number;
  relatedBizNo?: string;
  patientUuid?: string;
  clinicalContext?: Record<string, unknown>;
  taskNo?: string;
}

export interface PatientExplanationCommandDTO {
  question?: string;
  kbCode?: string;
  topK?: number;
  relatedBizNo?: string;
  patientUuid?: string;
  caseSummary?: Record<string, unknown>;
  riskLevelCode?: string;
  taskNo?: string;
}
