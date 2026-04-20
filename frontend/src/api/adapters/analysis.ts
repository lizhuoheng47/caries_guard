import type { AnalysisDetailViewDTO, AnalysisTaskListItemDTO, AnalysisTaskPageDTO } from '../dto/analysis';
import type {
  AnalysisAsset,
  AnalysisCitation,
  AnalysisDetail,
  AnalysisLesion,
  AnalysisTaskItem,
  AnalysisTreatmentItem,
} from '../../models/analysis';
import type { PaginatedList } from '../../models/base';

const normalizeTaskStatus = (status?: string): AnalysisTaskItem['status'] => {
  switch ((status || '').toUpperCase()) {
    case 'SUCCESS':
    case 'DONE':
      return 'DONE';
    case 'RUNNING':
      return 'RUNNING';
    case 'REVIEW':
      return 'REVIEW';
    case 'FAILED':
      return 'FAILED';
    default:
      return 'QUEUED';
  }
};

const normalizeRiskFactors = (riskFactors: any): string[] => {
  if (!Array.isArray(riskFactors)) return [];
  return riskFactors
    .map((item) => {
      if (typeof item === 'string') return item;
      if (item && typeof item === 'object') {
        return item.factorName || item.factorCode || item.summary || item.label;
      }
      return undefined;
    })
    .filter((item): item is string => Boolean(item));
};

const normalizeAssets = (assets: any): AnalysisAsset[] => {
  if (!Array.isArray(assets)) return [];
  return assets.map((asset) => ({
    type: asset.assetTypeCode || 'UNKNOWN',
    label: asset.assetTypeLabel || asset.assetTypeCode || 'Asset',
    url: asset.accessUrl,
    toothCode: asset.toothCode,
    attachmentId: asset.attachmentId,
  }));
};

const normalizeCitations = (citations: any): AnalysisCitation[] => {
  if (!Array.isArray(citations)) return [];
  return citations.map((citation, index) => ({
    index: Number(citation.rankNo ?? citation.docNo ?? index + 1),
    title: citation.docTitle || citation.title || `Citation ${index + 1}`,
    excerpt: citation.chunkText || citation.content,
    score: typeof citation.score === 'number' ? citation.score : undefined,
    sourceUri: citation.sourceUri,
  }));
};

const normalizeTreatmentPlan = (plan: any): AnalysisTreatmentItem[] => {
  if (!Array.isArray(plan)) return [];
  return plan
    .map((item) => {
      if (!item || typeof item !== 'object') return null;
      const details = String(item.details || item.description || '').trim();
      if (!details) return null;
      return {
        priority: String(item.priority || 'MEDIUM').toUpperCase(),
        title: String(item.title || 'Clinical follow-up'),
        details,
      };
    })
    .filter((item): item is AnalysisTreatmentItem => Boolean(item));
};

const normalizeLesions = (lesions: any): AnalysisLesion[] => {
  if (!Array.isArray(lesions)) return [];
  return lesions.map((lesion, index) => ({
    id: lesion.id || `lesion-${index + 1}`,
    toothCode: lesion.toothCode,
    severityCode: lesion.severityCode,
    confidence: typeof lesion.confidenceScore === 'number' ? lesion.confidenceScore : undefined,
    uncertainty: typeof lesion.uncertaintyScore === 'number' ? lesion.uncertaintyScore : undefined,
    areaPx: typeof lesion.lesionAreaPx === 'number' ? lesion.lesionAreaPx : undefined,
    areaRatio: typeof lesion.lesionAreaRatio === 'number' ? lesion.lesionAreaRatio : undefined,
    bbox: Array.isArray(lesion.bbox) ? lesion.bbox.map((value: any) => Number(value)) : undefined,
    polygon: Array.isArray(lesion.polygon)
      ? lesion.polygon.map((point: any) => (Array.isArray(point) ? point.map((value: any) => Number(value)) : []))
      : undefined,
    summary: lesion.summary,
    treatmentSuggestion: lesion.treatmentSuggestion,
  }));
};

export const AnalysisAdapter = {
  toTaskItem(dto: AnalysisTaskListItemDTO): AnalysisTaskItem {
    return {
      id: Number(dto.taskId),
      no: dto.taskNo,
      caseNo: undefined,
      grade: undefined,
      uncertainty: undefined,
      status: normalizeTaskStatus(dto.taskStatusCode),
      createdAt: dto.createdAt,
      duration: dto.inferenceMillis,
      needsReview: normalizeTaskStatus(dto.taskStatusCode) === 'REVIEW',
    };
  },

  toTaskList(dto: AnalysisTaskPageDTO): PaginatedList<AnalysisTaskItem> {
    const records = dto.records || dto.list || [];
    return {
      items: records.map((item) => this.toTaskItem(item)),
      total: dto.total,
      page: dto.pageNo || dto.pageNum || 1,
      pageSize: dto.pageSize || 10,
    };
  },

  toDetail(dto: AnalysisDetailViewDTO): AnalysisDetail {
    const raw = dto.rawResultJson || dto.analysisSummary?.rawResultJson || {};
    const visualAssets = normalizeAssets(dto.task.visualAssets);
    const citations = normalizeCitations(dto.analysisSummary?.citations || raw.citations);
    const ragCitations = normalizeCitations(dto.ragHint?.latestCitations);
    const lesions = normalizeLesions(raw.lesionResults);

    return {
      task: {
        id: Number(dto.task.taskId),
        no: dto.task.taskNo,
        status: dto.task.taskStatusCode,
        createdAt: dto.task.createdAt,
        completedAt: dto.task.completedAt,
        inferenceMillis: dto.task.inferenceMillis,
        visualAssets,
      },
      patient: {
        idMasked: dto.patient?.patientIdMasked,
        name: dto.patient?.patientNameMasked,
        gender: dto.patient?.gender,
        age: dto.patient?.age,
      },
      caseInfo: {
        id: dto.caseInfo?.caseId,
        no: dto.caseInfo?.caseNo,
        visitTime: dto.caseInfo?.visitTime,
      },
      image: {
        id: dto.image?.imageId,
        url: dto.image?.imageUrl,
        sourceDevice: dto.image?.sourceDevice,
      },
      summary: {
        grade: dto.analysisSummary?.gradingLabel || raw.gradingLabel || dto.analysisSummary?.overallHighestSeverity || 'UNKNOWN',
        confidence: dto.analysisSummary?.confidenceScore ?? raw.confidenceScore,
        uncertainty: dto.analysisSummary?.uncertaintyScore ?? raw.uncertaintyScore,
        needsReview: Boolean(dto.analysisSummary?.needsReview ?? raw.needsReview),
        riskLevel: dto.analysisSummary?.riskLevelLabel || dto.analysisSummary?.riskLevel || raw.riskLevel,
        riskFactors: normalizeRiskFactors(raw.riskFactors),
        lesionCount: Number(dto.analysisSummary?.lesionCount ?? raw.lesionCount ?? lesions.length ?? 0),
        abnormalToothCount: Number(dto.analysisSummary?.abnormalToothCount ?? raw.abnormalToothCount ?? 0),
        clinicalSummary: raw.clinicalSummary,
        followUpRecommendation:
          dto.analysisSummary?.followUpRecommendation ||
          raw.followUpRecommendation ||
          dto.ragHint?.latestAnswer,
        knowledgeVersion:
          dto.analysisSummary?.knowledgeVersion ||
          raw.knowledgeAdvice?.knowledgeVersion ||
          raw.knowledgeVersion,
        treatmentPlan: normalizeTreatmentPlan(raw.treatmentPlan),
        lesions,
        citations,
        annotationImageWidth:
          typeof raw.annotationImageWidth === "number" ? raw.annotationImageWidth : undefined,
        annotationImageHeight:
          typeof raw.annotationImageHeight === "number" ? raw.annotationImageHeight : undefined,
        rawResultJson: raw,
      },
      timeline: (dto.timeline || []).map((item, index) => ({
        code: `timeline-${index + 1}`,
        name: item.title || `Step ${index + 1}`,
        status: item.status === 'ACTIVE' ? 'CURRENT' : item.status === 'DONE' ? 'COMPLETED' : 'PENDING',
        time: item.time,
        description: item.content,
      })),
      rag: {
        enabled: Boolean(dto.ragHint?.enabled),
        answer: dto.ragHint?.latestAnswer || raw.followUpRecommendation,
        citations: ragCitations.length ? ragCitations : citations,
      },
    };
  },
};
