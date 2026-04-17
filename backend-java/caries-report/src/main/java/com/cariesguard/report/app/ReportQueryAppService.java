package com.cariesguard.report.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.interfaces.vo.ReportAnalysisSummaryVO;
import com.cariesguard.report.interfaces.vo.ReportCorrectionVO;
import com.cariesguard.report.interfaces.vo.ReportDetailVO;
import com.cariesguard.report.interfaces.vo.ReportImageVO;
import com.cariesguard.report.interfaces.vo.ReportListItemVO;
import com.cariesguard.report.interfaces.vo.ReportRiskAssessmentVO;
import com.cariesguard.report.interfaces.vo.ReportToothRecordVO;
import com.cariesguard.report.interfaces.vo.ReportVisualAssetVO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReportQueryAppService {

    private final ReportRecordRepository reportRecordRepository;
    private final ReportSourceQueryRepository reportSourceQueryRepository;

    public ReportQueryAppService(ReportRecordRepository reportRecordRepository,
                                 ReportSourceQueryRepository reportSourceQueryRepository) {
        this.reportRecordRepository = reportRecordRepository;
        this.reportSourceQueryRepository = reportSourceQueryRepository;
    }

    public List<ReportListItemVO> listCaseReports(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());
        return reportRecordRepository.listByCaseId(caseId).stream()
                .map(this::toListItemVO)
                .toList();
    }

    public ReportDetailVO getReport(Long reportId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportRecordModel report = reportRecordRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report does not exist"));
        ensureOrgAccess(operator, report.orgId());
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(report.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        Optional<ReportAnalysisSummaryModel> summary = report.sourceSummaryId() == null
                ? reportSourceQueryRepository.findLatestSummary(report.caseId())
                : reportSourceQueryRepository.findSummaryById(report.sourceSummaryId());
        Optional<ReportRiskAssessmentModel> riskAssessment = report.sourceRiskAssessmentId() == null
                ? reportSourceQueryRepository.findLatestRiskAssessment(report.caseId())
                : reportSourceQueryRepository.findRiskAssessmentById(report.sourceRiskAssessmentId());
        List<ReportImageModel> images = reportSourceQueryRepository.listCaseImages(report.caseId());
        List<ReportToothRecordModel> toothRecords = reportSourceQueryRepository.listToothRecords(report.caseId());
        List<ReportVisualAssetModel> visualAssets = summary
                .map(item -> reportSourceQueryRepository.listVisualAssetsByTaskId(item.taskId()))
                .orElseGet(List::of);
        List<ReportCorrectionModel> corrections = reportSourceQueryRepository.listCorrections(report.caseId());
        return new ReportDetailVO(
                report.reportId(),
                report.reportNo(),
                report.caseId(),
                medicalCase.caseNo(),
                report.patientId(),
                report.attachmentId(),
                report.sourceSummaryId(),
                report.sourceRiskAssessmentId(),
                report.sourceCorrectionId(),
                report.reportTypeCode(),
                report.reportStatusCode(),
                report.versionNo(),
                report.summaryText(),
                summary.map(this::toSummaryVO).orElse(null),
                riskAssessment.map(this::toRiskAssessmentVO).orElse(null),
                images.stream().map(this::toImageVO).toList(),
                toothRecords.stream().map(this::toToothRecordVO).toList(),
                visualAssets.stream().map(this::toVisualAssetVO).toList(),
                corrections.stream().map(this::toCorrectionVO).toList(),
                report.generatedAt(),
                report.signedAt(),
                report.createdAt());
    }

    private ReportListItemVO toListItemVO(ReportRecordModel report) {
        return new ReportListItemVO(
                report.reportId(),
                report.reportNo(),
                report.reportTypeCode(),
                report.versionNo(),
                report.reportStatusCode(),
                report.attachmentId(),
                report.generatedAt(),
                report.createdAt());
    }

    private ReportAnalysisSummaryVO toSummaryVO(ReportAnalysisSummaryModel model) {
        return new ReportAnalysisSummaryVO(
                model.summaryId(),
                model.taskId(),
                model.overallHighestSeverity(),
                model.uncertaintyScore(),
                model.reviewSuggestedFlag(),
                model.lesionCount(),
                model.abnormalToothCount());
    }

    private ReportRiskAssessmentVO toRiskAssessmentVO(ReportRiskAssessmentModel model) {
        return new ReportRiskAssessmentVO(
                model.riskAssessmentId(),
                model.overallRiskLevelCode(),
                model.assessmentReportJson(),
                model.recommendedCycleDays(),
                model.assessedAt());
    }

    private ReportImageVO toImageVO(ReportImageModel model) {
        return new ReportImageVO(
                model.imageId(),
                model.attachmentId(),
                model.imageTypeCode(),
                model.qualityStatusCode(),
                model.primaryFlag(),
                model.bucketName(),
                model.objectKey(),
                model.originalName());
    }

    private ReportToothRecordVO toToothRecordVO(ReportToothRecordModel model) {
        return new ReportToothRecordVO(
                model.toothRecordId(),
                model.sourceImageId(),
                model.toothCode(),
                model.toothSurfaceCode(),
                model.issueTypeCode(),
                model.severityCode(),
                model.findingDesc(),
                model.suggestion(),
                model.sortOrder());
    }

    private ReportVisualAssetVO toVisualAssetVO(ReportVisualAssetModel model) {
        return new ReportVisualAssetVO(
                model.visualAssetId(),
                model.taskId(),
                model.assetTypeCode(),
                model.attachmentId(),
                model.relatedImageId(),
                model.sourceAttachmentId(),
                model.toothCode(),
                model.sortOrder(),
                model.bucketName(),
                model.objectKey(),
                model.contentType(),
                model.originalName());
    }

    private ReportCorrectionVO toCorrectionVO(ReportCorrectionModel model) {
        return new ReportCorrectionVO(
                model.correctionId(),
                model.feedbackTypeCode(),
                model.correctedTruthJson(),
                model.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
