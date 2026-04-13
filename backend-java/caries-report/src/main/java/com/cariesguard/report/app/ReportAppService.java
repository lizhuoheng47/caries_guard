package com.cariesguard.report.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportAttachmentCreateModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.domain.model.ReportGenerateModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.repository.ReportExportLogRepository;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.domain.service.ReportDomainService;
import com.cariesguard.report.infrastructure.service.ReportPdfService;
import com.cariesguard.report.infrastructure.service.ReportRenderService;
import com.cariesguard.report.infrastructure.service.ReportTemplateResolver;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.followup.app.FollowupTriggerService;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReportAppService {

    private final ReportSourceQueryRepository reportSourceQueryRepository;
    private final ReportRecordRepository reportRecordRepository;
    private final ReportExportLogRepository reportExportLogRepository;
    private final ReportDomainService reportDomainService;
    private final ReportTemplateResolver reportTemplateResolver;
    private final ReportRenderService reportRenderService;
    private final ReportPdfService reportPdfService;
    private final ObjectStorageService objectStorageService;
    private final CaseCommandAppService caseCommandAppService;
    private final FollowupTriggerService followupTriggerService;

    public ReportAppService(ReportSourceQueryRepository reportSourceQueryRepository,
                            ReportRecordRepository reportRecordRepository,
                            ReportExportLogRepository reportExportLogRepository,
                            ReportDomainService reportDomainService,
                            ReportTemplateResolver reportTemplateResolver,
                            ReportRenderService reportRenderService,
                            ReportPdfService reportPdfService,
                            ObjectStorageService objectStorageService,
                            CaseCommandAppService caseCommandAppService,
                            FollowupTriggerService followupTriggerService) {
        this.reportSourceQueryRepository = reportSourceQueryRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.reportExportLogRepository = reportExportLogRepository;
        this.reportDomainService = reportDomainService;
        this.reportTemplateResolver = reportTemplateResolver;
        this.reportRenderService = reportRenderService;
        this.reportPdfService = reportPdfService;
        this.objectStorageService = objectStorageService;
        this.caseCommandAppService = caseCommandAppService;
        this.followupTriggerService = followupTriggerService;
    }

    @Transactional
    public ReportGenerateResultVO generateReport(Long caseId, GenerateReportCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        String reportTypeCode = reportDomainService.normalizeReportType(command.reportTypeCode());
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());
        reportDomainService.ensureCaseStatusAllowed(medicalCase.caseStatusCode());

        ReportAnalysisSummaryModel summary = reportSourceQueryRepository.findLatestSummary(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis summary does not exist"));
        List<ReportImageModel> images = reportSourceQueryRepository.listCaseImages(caseId);
        Optional<ReportRiskAssessmentModel> riskAssessment = reportSourceQueryRepository.findLatestRiskAssessment(caseId);
        Optional<ReportCorrectionModel> correction = reportSourceQueryRepository.findLatestCorrection(caseId);
        LocalDateTime generatedAt = LocalDateTime.now();

        long reportId = IdWorker.getId();
        String reportNo = reportDomainService.buildReportNo(reportId);
        int versionNo = reportRecordRepository.nextVersionNo(caseId, reportTypeCode);
        ReportRenderDataModel renderData = new ReportRenderDataModel(
                medicalCase.caseNo(),
                medicalCase.caseId(),
                medicalCase.patientId(),
                reportTypeCode,
                images.size(),
                summary.overallHighestSeverity(),
                summary.uncertaintyScore(),
                riskAssessment.map(ReportRiskAssessmentModel::overallRiskLevelCode).orElse(null),
                riskAssessment.map(ReportRiskAssessmentModel::recommendedCycleDays).orElse(null),
                summary.reviewSuggestedFlag(),
                correction.map(ReportCorrectionModel::correctedTruthJson).orElse(null),
                trimToNull(command.doctorConclusion()),
                generatedAt);

        ReportGenerateModel draftRecord = new ReportGenerateModel(
                reportId,
                reportNo,
                caseId,
                medicalCase.patientId(),
                reportTypeCode,
                versionNo,
                reportDomainService.draftStatus(),
                reportDomainService.buildSummaryText(renderData),
                null,
                medicalCase.orgId(),
                "ACTIVE",
                trimToNull(command.remark()),
                operator.getUserId());
        reportRecordRepository.create(draftRecord);

        StoredObject storedObject = null;
        try {
            String templateContent = reportTemplateResolver.resolveContent(medicalCase.orgId(), reportTypeCode);
            String renderedContent = reportRenderService.render(templateContent, reportNo, renderData);
            byte[] pdfBytes = reportPdfService.generatePdf(renderedContent);
            storedObject = storeReportPdf(reportNo, pdfBytes);
            long attachmentId = IdWorker.getId();
            reportRecordRepository.createAttachment(new ReportAttachmentCreateModel(
                    attachmentId,
                    "REPORT",
                    reportId,
                    "REPORT",
                    storedObject.fileName(),
                    reportNo + ".pdf",
                    storedObject.bucketName(),
                    storedObject.objectKey(),
                    "application/pdf",
                    "pdf",
                    storedObject.fileSizeBytes(),
                    storedObject.md5(),
                    storedObject.providerCode(),
                    "PRIVATE",
                    operator.getUserId(),
                    medicalCase.orgId(),
                    "ACTIVE",
                    "Generated report file",
                    operator.getUserId()));
            reportRecordRepository.updateArchiveInfo(
                    reportId,
                    attachmentId,
                    reportDomainService.finalStatus(),
                    generatedAt,
                    operator.getUserId());
            transitionCaseToReportReady(medicalCase.caseId(), medicalCase.caseStatusCode());
            triggerFollowupIfNeeded(reportId, medicalCase, renderData, riskAssessment.orElse(null), operator.getUserId());
            return new ReportGenerateResultVO(
                    reportId,
                    reportNo,
                    reportTypeCode,
                    versionNo,
                    reportDomainService.finalStatus());
        } catch (RuntimeException exception) {
            deleteStoredObjectQuietly(storedObject);
            throw exception;
        }
    }

    @Transactional
    public ReportExportResultVO exportReport(Long reportId, ExportReportCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportRecordModel report = reportRecordRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report does not exist"));
        ensureOrgAccess(operator, report.orgId());
        if (report.attachmentId() == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report archive does not exist");
        }
        reportRecordRepository.findAttachment(report.attachmentId())
                .filter(attachment -> operator.hasAnyRole("ADMIN", "SYS_ADMIN") || attachment.orgId().equals(operator.getOrgId()))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report attachment does not exist"));
        long exportLogId = IdWorker.getId();
        reportExportLogRepository.create(new ReportExportLogModel(
                exportLogId,
                report.reportId(),
                report.attachmentId(),
                reportDomainService.normalizeExportType(command == null ? null : command.exportTypeCode()),
                reportDomainService.normalizeExportChannel(command == null ? null : command.exportChannelCode()),
                operator.getUserId(),
                LocalDateTime.now(),
                report.orgId()));
        return new ReportExportResultVO(report.reportId(), true, exportLogId);
    }

    private StoredObject storeReportPdf(String reportNo, byte[] pdfBytes) {
        try {
            return objectStorageService.store(
                    reportNo + ".pdf",
                    "application/pdf",
                    new ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    md5(pdfBytes));
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Store report file failed");
        }
    }

    private void deleteStoredObjectQuietly(StoredObject storedObject) {
        if (storedObject == null) {
            return;
        }
        try {
            objectStorageService.delete(storedObject.bucketName(), storedObject.objectKey());
        } catch (IOException ignored) {
            // Keep the original exception.
        }
    }

    private void triggerFollowupIfNeeded(Long reportId,
                                         ReportCaseModel medicalCase,
                                         ReportRenderDataModel renderData,
                                         ReportRiskAssessmentModel riskAssessment,
                                         Long operatorUserId) {
        String riskLevelCode = riskAssessment != null ? riskAssessment.overallRiskLevelCode() : null;
        String reviewSuggestedFlag = renderData.reviewSuggestedFlag();
        Integer recommendedCycleDays = riskAssessment != null ? riskAssessment.recommendedCycleDays() : null;
        followupTriggerService.triggerFromReport(
                medicalCase.caseId(),
                medicalCase.patientId(),
                medicalCase.orgId(),
                reportId,
                riskLevelCode,
                reviewSuggestedFlag,
                recommendedCycleDays,
                operatorUserId);
    }

    private void transitionCaseToReportReady(Long caseId, String currentCaseStatusCode) {
        if (!"REVIEW_PENDING".equals(currentCaseStatusCode)) {
            return;
        }
        caseCommandAppService.transitionStatus(caseId, new CaseStatusTransitionCommand(
                "REPORT_READY",
                "DOCTOR_CONFIRMED",
                "Report generated"));
    }

    private String md5(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
