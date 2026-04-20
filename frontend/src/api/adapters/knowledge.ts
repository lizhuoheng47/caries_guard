import type {
  KnowledgeDocumentDetailDTO,
  KnowledgeDocumentListItemDTO,
  KnowledgeDocumentsResultDTO,
  KnowledgePublishRecordDTO,
  KnowledgeReviewRecordDTO,
  KnowledgeDocumentVersionDTO,
  KbOverviewDTO,
} from '../dto/knowledge';
import type {
  KnowledgeDocument,
  KnowledgeDocumentDetail,
  KnowledgeDocumentVersion,
  KnowledgePublishRecord,
  KnowledgeReviewRecord,
  KbStats,
} from '../../models/knowledge';
import type { PaginatedList } from '../../models/base';

type AnyRecord = Record<string, unknown>;

const asRecord = (value: unknown): AnyRecord =>
  value !== null && typeof value === 'object' ? (value as AnyRecord) : {};

const readString = (obj: AnyRecord, ...keys: string[]): string | undefined => {
  for (const key of keys) {
    const value = obj[key];
    if (typeof value === 'string' && value.trim() !== '') {
      return value;
    }
  }
  return undefined;
};

const readNumber = (obj: AnyRecord, ...keys: string[]): number | undefined => {
  for (const key of keys) {
    const value = obj[key];
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === 'string' && value.trim() !== '') {
      const numeric = Number(value);
      if (Number.isFinite(numeric)) {
        return numeric;
      }
    }
  }
  return undefined;
};

const toVersion = (dto: KnowledgeDocumentVersionDTO): KnowledgeDocumentVersion => {
  const record = asRecord(dto);
  return {
    versionNo: readString(record, 'versionNo', 'version_no') ?? 'v1.0',
    parentVersionNo: readString(record, 'parentVersionNo', 'parent_version_no'),
    reviewStatus: readString(record, 'reviewStatusCode', 'review_status_code') ?? 'PENDING',
    publishStatus: readString(record, 'publishStatusCode', 'publish_status_code') ?? 'DRAFT',
    changeSummary: readString(record, 'changeSummary', 'change_summary'),
    createdAt: readString(record, 'createdAt', 'created_at'),
    updatedAt: readString(record, 'updatedAt', 'updated_at'),
  };
};

const toReviewRecord = (dto: KnowledgeReviewRecordDTO): KnowledgeReviewRecord => {
  const record = asRecord(dto);
  return {
    versionNo: readString(record, 'versionNo', 'version_no') ?? '-',
    decisionCode: readString(record, 'decisionCode', 'decision_code') ?? 'UNKNOWN',
    reviewComment: readString(record, 'reviewComment', 'review_comment'),
    reviewedAt: readString(record, 'reviewedAt', 'reviewed_at'),
  };
};

const toPublishRecord = (dto: KnowledgePublishRecordDTO): KnowledgePublishRecord => {
  const record = asRecord(dto);
  return {
    versionNo: readString(record, 'versionNo', 'version_no') ?? '-',
    previousVersionNo: readString(record, 'previousVersionNo', 'previous_version_no'),
    actionCode: readString(record, 'actionCode', 'action_code') ?? 'UNKNOWN',
    commentText: readString(record, 'commentText', 'comment_text'),
    publishedAt: readString(record, 'publishedAt', 'published_at'),
  };
};

export const KnowledgeAdapter = {
  toStats(dto: KbOverviewDTO): KbStats {
    const record = asRecord(dto);
    const latestRebuildJob = asRecord(record.latestRebuildJob ?? record.latest_rebuild_job);

    return {
      docs: readNumber(record, 'totalDocs', 'documentCount', 'docCount') ?? 0,
      chunks: readNumber(record, 'totalChunks', 'chunkCount') ?? 0,
      lastIndexed:
        readString(record, 'lastIndexedAt', 'lastIndexed') ??
        readString(
          latestRebuildJob,
          'finishedAt',
          'finished_at',
          'updatedAt',
          'updated_at',
          'createdAt',
          'created_at',
        ) ??
        '',
      version:
        readString(record, 'currentVersion', 'knowledgeVersion') ??
        readString(latestRebuildJob, 'knowledgeVersion', 'knowledge_version') ??
        '--',
      entities: readNumber(record, 'entityCount'),
      relations: readNumber(record, 'relationCount'),
    };
  },

  toDocument(dto: KnowledgeDocumentListItemDTO): KnowledgeDocument {
    const record = asRecord(dto);
    const currentVersionNo = readString(record, 'currentVersionNo', 'current_version_no');
    const publishedVersionNo = readString(record, 'publishedVersionNo', 'published_version_no');

    return {
      id: readNumber(record, 'docId', 'doc_id', 'id') ?? 0,
      no: readString(record, 'docNo', 'doc_no') ?? '-',
      title: readString(record, 'docTitle', 'doc_title') ?? '-',
      type: readString(record, 'docSourceCode', 'doc_source_code') ?? 'UNKNOWN',
      sourceUri: readString(record, 'sourceUri', 'source_uri'),
      reviewStatus: readString(record, 'reviewStatusCode', 'review_status_code') ?? 'PENDING',
      publishStatus: readString(record, 'publishStatusCode', 'publish_status_code') ?? 'DRAFT',
      version:
        publishedVersionNo ??
        currentVersionNo ??
        readString(record, 'docVersion', 'doc_version') ??
        undefined,
      currentVersionNo,
      publishedVersionNo,
      chunks: readNumber(record, 'chunkCount', 'chunk_count'),
      entities: readNumber(record, 'entityCount', 'entity_count'),
      relations: readNumber(record, 'relationCount', 'relation_count'),
      updatedAt:
        readString(record, 'updatedAt', 'updated_at', 'createdAt', 'created_at') ??
        new Date().toISOString(),
    };
  },

  toDocumentList(
    dto: KnowledgeDocumentsResultDTO,
    params: { pageNum?: number; pageSize?: number } = {},
  ): PaginatedList<KnowledgeDocument> {
    if (Array.isArray(dto)) {
      const page = params.pageNum && params.pageNum > 0 ? params.pageNum : 1;
      const pageSize = params.pageSize && params.pageSize > 0 ? params.pageSize : 10;
      const total = dto.length;
      const start = (page - 1) * pageSize;
      const list = dto.slice(start, start + pageSize);

      return {
        items: list.map((item) => this.toDocument(item)),
        total,
        page,
        pageSize,
      };
    }

    const record = asRecord(dto);
    const list = Array.isArray(record.list) ? (record.list as KnowledgeDocumentListItemDTO[]) : [];
    const page = readNumber(record, 'pageNum', 'pageNo', 'page_no') ?? params.pageNum ?? 1;
    const pageSize = readNumber(record, 'pageSize') ?? params.pageSize ?? 10;
    const total = readNumber(record, 'total', 'totalCount') ?? list.length;

    return {
      items: list.map((item) => this.toDocument(item)),
      total,
      page,
      pageSize,
    };
  },

  toDocumentDetail(dto: KnowledgeDocumentDetailDTO): KnowledgeDocumentDetail {
    const record = asRecord(dto);
    const base = this.toDocument(dto);
    const versionsRaw = Array.isArray(record.versions) ? (record.versions as KnowledgeDocumentVersionDTO[]) : [];
    const reviewRecordsRaw = Array.isArray(record.reviewRecords)
      ? (record.reviewRecords as KnowledgeReviewRecordDTO[])
      : [];
    const publishRecordsRaw = Array.isArray(record.publishRecords)
      ? (record.publishRecords as KnowledgePublishRecordDTO[])
      : [];
    const currentVersionRaw = asRecord(record.currentVersion ?? record.current_version);

    return {
      ...base,
      contentText: readString(record, 'contentText', 'content_text'),
      versions: versionsRaw.map((item) => toVersion(item)),
      reviewRecords: reviewRecordsRaw.map((item) => toReviewRecord(item)),
      publishRecords: publishRecordsRaw.map((item) => toPublishRecord(item)),
      currentVersion: Object.keys(currentVersionRaw).length
        ? toVersion(currentVersionRaw as KnowledgeDocumentVersionDTO)
        : null,
    };
  },
};
