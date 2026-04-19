export interface KnowledgeDocumentListItemDTO {
  docId: number;
  docNo: string;
  docTitle: string;
  docSourceCode: string;
  reviewStatusCode: string;
  publishStatusCode: string;
  currentVersionNo?: string;
  publishedVersionNo?: string;
  chunkCount?: number;
  entityCount?: number;
  relationCount?: number;
  updatedAt: string;
}

export interface KbOverviewDTO {
  totalDocs: number;
  totalChunks: number;
  lastIndexedAt: string;
  currentVersion: string;
}
