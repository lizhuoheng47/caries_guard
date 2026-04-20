export interface KnowledgeDocumentListItemDTO {
  docId?: number;
  doc_id?: number;
  id?: number;
  docNo?: string;
  doc_no?: string;
  docTitle?: string;
  doc_title?: string;
  docSourceCode?: string;
  doc_source_code?: string;
  sourceUri?: string;
  source_uri?: string;
  reviewStatusCode?: string;
  review_status_code?: string;
  publishStatusCode?: string;
  publish_status_code?: string;
  currentVersionNo?: string;
  current_version_no?: string;
  publishedVersionNo?: string;
  published_version_no?: string;
  docVersion?: string;
  doc_version?: string;
  chunkCount?: number;
  chunk_count?: number;
  entityCount?: number;
  entity_count?: number;
  relationCount?: number;
  relation_count?: number;
  updatedAt?: string;
  updated_at?: string;
  createdAt?: string;
  created_at?: string;
}

export interface KbOverviewDTO {
  totalDocs?: number;
  totalChunks?: number;
  lastIndexedAt?: string;
  currentVersion?: string;
  documentCount?: number;
  chunkCount?: number;
  entityCount?: number;
  relationCount?: number;
  latestRebuildJob?: Record<string, unknown> | null;
  latest_rebuild_job?: Record<string, unknown> | null;
}

export interface KnowledgeDocumentVersionDTO {
  versionNo?: string;
  version_no?: string;
  parentVersionNo?: string;
  parent_version_no?: string;
  reviewStatusCode?: string;
  review_status_code?: string;
  publishStatusCode?: string;
  publish_status_code?: string;
  changeSummary?: string;
  change_summary?: string;
  createdAt?: string;
  created_at?: string;
  updatedAt?: string;
  updated_at?: string;
}

export interface KnowledgeReviewRecordDTO {
  versionNo?: string;
  version_no?: string;
  decisionCode?: string;
  decision_code?: string;
  reviewComment?: string;
  review_comment?: string;
  reviewedAt?: string;
  reviewed_at?: string;
}

export interface KnowledgePublishRecordDTO {
  versionNo?: string;
  version_no?: string;
  previousVersionNo?: string;
  previous_version_no?: string;
  actionCode?: string;
  action_code?: string;
  commentText?: string;
  comment_text?: string;
  publishedAt?: string;
  published_at?: string;
}

export interface KnowledgeDocumentDetailDTO extends KnowledgeDocumentListItemDTO {
  contentText?: string;
  content_text?: string;
  versions?: KnowledgeDocumentVersionDTO[];
  reviewRecords?: KnowledgeReviewRecordDTO[];
  publishRecords?: KnowledgePublishRecordDTO[];
  currentVersion?: KnowledgeDocumentVersionDTO | null;
  current_version?: KnowledgeDocumentVersionDTO | null;
}

export type KnowledgeDocumentsResultDTO =
  | {
      list: KnowledgeDocumentListItemDTO[];
      total?: number;
      pageNum?: number;
      pageSize?: number;
      pageNo?: number;
      page_no?: number;
      totalCount?: number;
    }
  | KnowledgeDocumentListItemDTO[];

export interface KnowledgeUploadResultDTO {
  docId?: number;
  docNo?: string;
  docTitle?: string;
  versionNo?: string;
  reviewStatusCode?: string;
  publishStatusCode?: string;
  chunkCount?: number;
  entityCount?: number;
  relationCount?: number;
  ingestJobNo?: string;
}

export interface KnowledgeRebuildResultDTO {
  rebuildJobNo?: string;
  rebuildStatusCode?: string;
  chunkCount?: number;
}

export interface KnowledgeActionResultDTO {
  docId?: number;
  versionNo?: string;
  status?: string;
}
