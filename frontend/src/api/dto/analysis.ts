export interface AnalysisTaskListItemDTO {
  taskId: number;
  taskNo: string;
  patientNameMasked?: string;
  patientIdMasked?: string;
  caseNo: string;
  gradingLabel?: string;
  uncertaintyScore?: number;
  statusCode: 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED';
  createdAt: string;
  durationMs?: number;
  needsReview: boolean;
}

export interface AnalysisDetailViewDTO {
  task: {
    taskId: number;
    taskNo: string;
    statusCode: string;
    createdAt: string;
  };
  patient: {
    patientId: number;
    nameMasked: string;
    gender: string;
    age: number;
  };
  caseInfo: {
    caseId: number;
    caseNo: string;
    visitTime: string;
  };
  image: {
    imageId: number;
    imageUrl: string;
  };
  analysisSummary: {
    gradingLabel: string;
    confidenceScore?: number;
    uncertaintyScore?: number;
    needsReview: boolean;
    riskLevel?: string;
    riskFactors?: string[];
    visualAssets?: any[];
  };
  timeline: {
    nodeCode: string;
    nodeName: string;
    status: 'COMPLETED' | 'CURRENT' | 'PENDING';
    timestamp?: string;
  }[];
  ragHint?: {
    enabled: boolean;
    latestAnswer?: string;
  };
}
