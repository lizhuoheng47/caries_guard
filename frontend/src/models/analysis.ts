export interface AnalysisTaskItem {
  id: number;
  no: string;
  patientName?: string;
  patientId?: string;
  caseNo?: string;
  grade?: string;
  uncertainty?: number;
  status: 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED';
  createdAt: string;
  duration?: number;
  needsReview: boolean;
}

export interface TimelineNode {
  code: string;
  name: string;
  status: 'COMPLETED' | 'CURRENT' | 'PENDING';
  time?: string;
  description?: string;
}

export interface AnalysisAsset {
  type: string;
  label: string;
  url?: string;
  toothCode?: string;
  attachmentId?: number;
}

export interface AnalysisCitation {
  index: number;
  title: string;
  excerpt?: string;
  score?: number;
  sourceUri?: string;
}

export interface AnalysisTreatmentItem {
  priority: string;
  title: string;
  details: string;
}

export interface AnalysisLesion {
  id: string;
  toothCode?: string;
  severityCode?: string;
  confidence?: number;
  uncertainty?: number;
  areaPx?: number;
  areaRatio?: number;
  bbox?: number[];
  polygon?: number[][];
  summary?: string;
  treatmentSuggestion?: string;
}

export interface AnalysisDetail {
  task: {
    id: number;
    no: string;
    status: string;
    createdAt: string;
    completedAt?: string;
    inferenceMillis?: number;
    visualAssets: AnalysisAsset[];
  };
  patient: {
    idMasked?: string;
    name?: string;
    gender?: string;
    age?: number;
  };
  caseInfo: {
    id?: number;
    no?: string;
    visitTime?: string;
  };
  image: {
    id?: number;
    url?: string;
    sourceDevice?: string;
  };
  summary: {
    grade: string;
    confidence?: number;
    uncertainty?: number;
    needsReview: boolean;
    riskLevel?: string;
    riskFactors: string[];
    lesionCount: number;
    abnormalToothCount: number;
    clinicalSummary?: string;
    followUpRecommendation?: string;
    treatmentPlan: AnalysisTreatmentItem[];
    lesions: AnalysisLesion[];
    citations: AnalysisCitation[];
    annotationImageWidth?: number;
    annotationImageHeight?: number;
    rawResultJson?: Record<string, any> | null;
  };
  timeline: TimelineNode[];
}
