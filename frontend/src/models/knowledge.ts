export interface KnowledgeDocument {
  id: number;
  no: string;
  title: string;
  type: string;
  reviewStatus: string;
  publishStatus: string;
  version?: string;
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
}
