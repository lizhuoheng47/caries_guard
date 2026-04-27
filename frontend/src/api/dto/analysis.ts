export interface AnalysisTaskListItemDTO {
  taskId: number | string;
  taskNo: string;
  taskStatusCode: 'SUCCESS' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'PENDING' | string;
  taskTypeCode?: string;
  modelVersion?: string;
  caseNo?: string;
  patientName?: string;
  patientId?: string;
  gradingLabel?: string;
  uncertaintyScore?: number;
  needsReview?: boolean;
  errorCode?: string;
  errorMessage?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  traceId?: string;
  inferenceMillis?: number;
}

export interface AnalysisTaskPageDTO {
  pageNo?: number;
  pageSize?: number;
  total: number;
  records?: AnalysisTaskListItemDTO[];
  list?: AnalysisTaskListItemDTO[];
  pageNum?: number;
}

export interface AnalysisVisualAssetDTO {
  assetTypeCode: string;
  attachmentId?: number;
  relatedImageId?: number;
  sourceAttachmentId?: number;
  toothCode?: string;
  sortOrder?: number;
  accessUrl?: string;
  assetTypeLabel?: string;
}

export interface AnalysisCitationDTO {
  rankNo?: number;
  docTitle?: string;
  chunkText?: string;
  score?: number;
  sourceUri?: string;
}

export interface AnalysisSummaryDTO {
  overallHighestSeverity?: string;
  uncertaintyScore?: number;
  reviewSuggestedFlag?: string;
  lesionCount?: number;
  abnormalToothCount?: number;
  riskLevel?: string;
  riskLevelLabel?: string;
  gradingLabel?: string;
  confidenceScore?: number;
  needsReview?: boolean;
  followUpRecommendation?: string;
  citations?: AnalysisCitationDTO[];
  rawResultJson?: Record<string, any> | null;
}

export interface AnalysisDetailViewDTO {
  task: {
    taskId: number | string;
    taskNo: string;
    caseId: number;
    taskStatusCode: string;
    taskTypeCode?: string;
    modelVersion?: string;
    errorCode?: string;
    errorMessage?: string;
    createdAt: string;
    startedAt?: string;
    completedAt?: string;
    traceId?: string;
    inferenceMillis?: number;
    visualAssets?: AnalysisVisualAssetDTO[];
  };
  patient?: {
    patientIdMasked?: string;
    patientNameMasked?: string;
    gender?: string;
    age?: number;
  };
  caseInfo?: {
    caseId: number;
    caseNo?: string;
    visitTime?: string;
  };
  image?: {
    imageId?: number;
    imageUrl?: string;
    sourceDevice?: string;
  };
  analysisSummary?: AnalysisSummaryDTO;
  rawResultJson?: Record<string, any> | null;
  timeline?: {
    time?: string;
    title?: string;
    content?: string;
    status?: 'DONE' | 'ACTIVE' | 'PENDING' | string;
    duration?: string;
  }[];
}
