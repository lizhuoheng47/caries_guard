import type { AnalysisTaskListItemDTO, AnalysisDetailViewDTO } from '../dto/analysis';
import type { AnalysisTaskItem, AnalysisDetail } from '../../models/analysis';
import type { PageResult } from '../dto/base';
import type { PaginatedList } from '../../models/base';

export const AnalysisAdapter = {
  toTaskItem(dto: AnalysisTaskListItemDTO): AnalysisTaskItem {
    return {
      id: dto.taskId,
      no: dto.taskNo,
      patientName: dto.patientNameMasked,
      patientId: dto.patientIdMasked,
      caseNo: dto.caseNo,
      grade: dto.gradingLabel,
      uncertainty: dto.uncertaintyScore,
      status: dto.statusCode,
      createdAt: dto.createdAt,
      duration: dto.durationMs,
      needsReview: dto.needsReview,
    };
  },

  toTaskList(dto: PageResult<AnalysisTaskListItemDTO>): PaginatedList<AnalysisTaskItem> {
    return {
      items: dto.list.map(i => this.toTaskItem(i)),
      total: dto.total,
      page: dto.pageNum,
      pageSize: dto.pageSize,
    };
  },

  toDetail(dto: AnalysisDetailViewDTO): AnalysisDetail {
    return {
      task: { id: dto.task.taskId, no: dto.task.taskNo, status: dto.task.statusCode, createdAt: dto.task.createdAt },
      patient: { id: dto.patient.patientId, name: dto.patient.nameMasked, gender: dto.patient.gender, age: dto.patient.age },
      caseInfo: { id: dto.caseInfo.caseId, no: dto.caseInfo.caseNo, visitTime: dto.caseInfo.visitTime },
      image: { id: dto.image.imageId, url: dto.image.imageUrl },
      summary: {
        grade: dto.analysisSummary.gradingLabel,
        confidence: dto.analysisSummary.confidenceScore,
        uncertainty: dto.analysisSummary.uncertaintyScore,
        needsReview: dto.analysisSummary.needsReview,
        riskLevel: dto.analysisSummary.riskLevel,
        riskFactors: dto.analysisSummary.riskFactors,
        assets: dto.analysisSummary.visualAssets,
      },
      timeline: dto.timeline.map(t => ({ code: t.nodeCode, name: t.nodeName, status: t.status, time: t.timestamp })),
      rag: { enabled: dto.ragHint?.enabled || false, answer: dto.ragHint?.latestAnswer },
    };
  }
};
