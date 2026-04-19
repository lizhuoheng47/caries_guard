import type { KnowledgeDocumentListItemDTO, KbOverviewDTO } from '../dto/knowledge';
import type { KnowledgeDocument, KbStats } from '../../models/knowledge';
import type { PageResult } from '../dto/base';
import type { PaginatedList } from '../../models/base';

export const KnowledgeAdapter = {
  toStats(dto: KbOverviewDTO): KbStats {
    return {
      docs: dto.totalDocs,
      chunks: dto.totalChunks,
      lastIndexed: dto.lastIndexedAt,
      version: dto.currentVersion,
    };
  },

  toDocument(dto: KnowledgeDocumentListItemDTO): KnowledgeDocument {
    return {
      id: dto.docId,
      no: dto.docNo,
      title: dto.docTitle,
      type: dto.docSourceCode,
      reviewStatus: dto.reviewStatusCode,
      publishStatus: dto.publishStatusCode,
      version: dto.publishedVersionNo || dto.currentVersionNo,
      chunks: dto.chunkCount,
      entities: dto.entityCount,
      relations: dto.relationCount,
      updatedAt: dto.updatedAt,
    };
  },

  toDocumentList(dto: PageResult<KnowledgeDocumentListItemDTO>): PaginatedList<KnowledgeDocument> {
    return {
      items: dto.list.map(i => this.toDocument(i)),
      total: dto.total,
      page: dto.pageNum,
      pageSize: dto.pageSize,
    };
  }
};
