export interface KnowledgeDocument {
  id: number;
  no: string;
  title: string;
  type: string;
  sourceUri?: string;
  reviewStatus: string;
  publishStatus: string;
  version?: string;
  currentVersionNo?: string;
  publishedVersionNo?: string;
  chunks?: number;
  entities?: number;
  relations?: number;
  updatedAt: string;
}

export interface KbStats {
  docs: number;
  chunks: number;
  lastIndexed: string;
  version: string;
  entities?: number;
  relations?: number;
}

export interface KnowledgeDocumentVersion {
  versionNo: string;
  parentVersionNo?: string;
  reviewStatus: string;
  publishStatus: string;
  changeSummary?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeReviewRecord {
  versionNo: string;
  decisionCode: string;
  reviewComment?: string;
  reviewedAt?: string;
}

export interface KnowledgePublishRecord {
  versionNo: string;
  previousVersionNo?: string;
  actionCode: string;
  commentText?: string;
  publishedAt?: string;
}

export interface KnowledgeDocumentDetail extends KnowledgeDocument {
  contentText?: string;
  versions: KnowledgeDocumentVersion[];
  reviewRecords: KnowledgeReviewRecord[];
  publishRecords: KnowledgePublishRecord[];
  currentVersion?: KnowledgeDocumentVersion | null;
}
