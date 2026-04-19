export interface AnalysisTaskItem {
  id: number;
  no: string;
  patientName?: string;
  patientId?: string;
  caseNo: string;
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
}

export interface AnalysisDetail {
  task: { id: number; no: string; status: string; createdAt: string; };
  patient: { id: number; name: string; gender: string; age: number; };
  caseInfo: { id: number; no: string; visitTime: string; };
  image: { id: number; url: string; };
  summary: {
    grade: string;
    confidence?: number;
    uncertainty?: number;
    needsReview: boolean;
    riskLevel?: string;
    riskFactors?: string[];
    assets?: any[];
  };
  timeline: TimelineNode[];
  rag: { enabled: boolean; answer?: string; };
}
