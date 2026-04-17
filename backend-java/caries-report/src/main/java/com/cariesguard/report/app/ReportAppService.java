package com.cariesguard.report.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.followup.app.FollowupTriggerService;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.domain.model.ObjectStoreCommand;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportAttachmentCreateModel;
import com.cariesguard.report.domain.model.ReportAttachmentModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.domain.model.ReportGenerateModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import com.cariesguard.report.domain.repository.ReportExportLogRepository;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.domain.service.ReportDomainService;
import com.cariesguard.report.infrastructure.service.ReportPdfService;
import com.cariesguard.report.infrastructure.service.ReportRenderService;
import com.cariesguard.report.infrastructure.service.ReportTemplateResolver;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReportAppService {

    private static final String CATEGORY_REPORT = "REPORT";
    private static final String CATEGORY_EXPORT = "EXPORT";
    private static final String RETENTION_LONG_TERM = "LONG_TERM";
    private static final String RETENTION_EXPORT_TEMPORARY = "TEMP_7D";

    private final ReportSourceQueryRepository reportSourceQueryRepository;
    private final ReportRecordRepository reportRecordRepository;
    private final ReportExportLogRepository reportExportLogRepository;
    private final ReportDomainService reportDomainService;
    private final ReportTemplateResolver reportTemplateResolver;
    private final ReportRenderService reportRenderService;
    private final RagAppService ragAppService;
    private final ReportPdfService reportPdfService;
    private final ObjectStorageService objectStorageService;
    private final AttachmentAppService attachmentAppService;
    private final CaseCommandAppService caseCommandAppService;
    private final FollowupTriggerService followupTriggerService;

    @Autowired
    public ReportAppService(ReportSourceQueryRepository reportSourceQueryRepository,
                            ReportRecordRepository reportRecordRepository,
                            ReportExportLogRepository reportExportLogRepository,
                            ReportDomainService reportDomainService,
                            ReportTemplateResolver reportTemplateResolver,
                            ReportRenderService reportRenderService,
                            RagAppService ragAppService,
                            ReportPdfService reportPdfService,
                            ObjectStorageService objectStorageService,
                            AttachmentAppService attachmentAppService,
                            CaseCommandAppService caseCommandAppService,
                            FollowupTriggerService followupTriggerService) {
        this.reportSourceQueryRepository = reportSourceQueryRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.reportExportLogRepository = reportExportLogRepository;
        this.reportDomainService = reportDomainService;
        this.reportTemplateResolver = reportTemplateResolver;
        this.reportRenderService = reportRenderService;
        this.ragAppService = ragAppService;
        this.reportPdfService = reportPdfService;
        this.objectStorageService = objectStorageService;
        this.attachmentAppService = attachmentAppService;
        this.caseCommandAppService = caseCommandAppService;
        this.followupTriggerService = followupTriggerService;
    }

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
        this(reportSourceQueryRepository, reportRecordRepository, reportExportLogRepository,
                reportDomainService, reportTemplateResolver, reportRenderService, null, reportPdfService,
                objectStorageService, null, caseCommandAppService, followupTriggerService);
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
        List<ReportToothRecordModel> toothRecords = reportSourceQueryRepository.listToothRecords(caseId);
        List<ReportVisualAssetModel> visualAssets = reportSourceQueryRepository.listVisualAssetsByTaskId(summary.taskId());
        Optional<ReportRiskAssessmentModel> riskAssessment = reportSourceQueryRepository.findLatestRiskAssessment(caseId);
        List<ReportCorrectionModel> corrections = reportSourceQueryRepository.listCorrections(caseId);
        Optional<ReportCorrectionModel> latestCorrection = corrections.stream().reduce((first, second) -> second);
        LocalDateTime generatedAt = LocalDateTime.now();

        long reportId = IdWorker.getId();
        String reportNo = reportDomainService.buildReportNo(reportId);
        int versionNo = reportRecordRepository.nextVersionNo(caseId, reportTypeCode);
        ReportRenderDataModel renderData = new ReportRenderDataModel(
                medicalCase.caseNo(),
                medicalCase.caseId(),
                medicalCase.patientId(),
                reportTypeCode,
                images,
                toothRecords,
                visualAssets,
                summary.overallHighestSeverity(),
                summary.uncertaintyScore(),
                summary.lesionCount(),
                summary.abnormalToothCount(),
                riskAssessment.map(ReportRiskAssessmentModel::overallRiskLevelCode).orElse(null),
                riskAssessment.map(ReportRiskAssessmentModel::recommendedCycleDays).orElse(null),
                summary.reviewSuggestedFlag(),
                corrections,
                trimToNull(command.doctorConclusion()),
                null,
                generatedAt);
        renderData = renderData.withPatientExplanation(patientExplanation(reportTypeCode, reportNo, renderData));

        ReportGenerateModel draftRecord = new ReportGenerateModel(
                reportId,
                reportNo,
                caseId,
                medicalCase.patientId(),
                summary.summaryId(),
                riskAssessment.map(ReportRiskAssessmentModel::riskAssessmentId).orElse(null),
                latestCorrection.map(ReportCorrectionModel::correctionId).orElse(null),
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
            storedObject = storeReportPdf(medicalCase, reportNo, reportTypeCode, versionNo, pdfBytes);
            long attachmentId = IdWorker.getId();
            reportRecordRepository.createAttachment(new ReportAttachmentCreateModel(
                    attachmentId,
                    CATEGORY_REPORT,
                    reportId,
                    CATEGORY_REPORT,
                    reportAssetType(reportTypeCode),
                    null,
                    storedObject.fileName(),
                    storedObject.fileName(),
                    storedObject.bucketName(),
                    storedObject.objectKey(),
                    "application/pdf",
                    "pdf",
                    storedObject.fileSizeBytes(),
                    storedObject.md5(),
                    storedObject.providerCode(),
                    "PRIVATE",
                    RETENTION_LONG_TERM,
                    null,
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
        ReportAttachmentModel reportAttachment = reportRecordRepository.findAttachment(report.attachmentId())
                .filter(attachment -> operator.hasAnyRole("ADMIN", "SYS_ADMIN") || attachment.orgId().equals(operator.getOrgId()))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report attachment does not exist"));

        long exportLogId = IdWorker.getId();
        long exportAttachmentId = IdWorker.getId();
        LocalDateTime exportedAt = LocalDateTime.now();
        StoredObject exportedObject = null;
        try {
            exportedObject = copyReportToExportBucket(report, reportAttachment, operator.getUserId(), exportLogId);
            reportRecordRepository.createAttachment(new ReportAttachmentCreateModel(
                    exportAttachmentId,
                    CATEGORY_EXPORT,
                    exportLogId,
                    CATEGORY_EXPORT,
                    "EXPORT_FILE",
                    report.attachmentId(),
                    exportedObject.fileName(),
                    exportedObject.fileName(),
                    exportedObject.bucketName(),
                    exportedObject.objectKey(),
                    exportedObject.contentType(),
                    "pdf",
                    exportedObject.fileSizeBytes(),
                    exportedObject.md5(),
                    exportedObject.providerCode(),
                    "PRIVATE",
                    RETENTION_EXPORT_TEMPORARY,
                    exportedAt.plusDays(7),
                    operator.getUserId(),
                    report.orgId(),
                    "ACTIVE",
                    "Exported report file",
                    operator.getUserId()));
            reportExportLogRepository.create(new ReportExportLogModel(
                    exportLogId,
                    report.reportId(),
                    exportAttachmentId,
                    reportDomainService.normalizeExportType(command == null ? null : command.exportTypeCode()),
                    reportDomainService.normalizeExportChannel(command == null ? null : command.exportChannelCode()),
                    operator.getUserId(),
                    exportedAt,
                    report.orgId()));
            AttachmentAccessVO downloadAccess = attachmentAppService == null
                    ? new AttachmentAccessVO(null, 0L)
                    : attachmentAppService.createAccessUrl(exportAttachmentId, (String) null);
            return new ReportExportResultVO(report.reportId(), true, exportLogId, exportAttachmentId, downloadAccess.accessUrl(), downloadAccess.expireAt());
        } catch (RuntimeException exception) {
            deleteStoredObjectQuietly(exportedObject);
            throw exception;
        }
    }

    private StoredObject storeReportPdf(ReportCaseModel medicalCase,
                                        String reportNo,
                                        String reportTypeCode,
                                        int versionNo,
                                        byte[] pdfBytes) {
        String fileName = "%s.pdf".formatted(reportNo);
        try {
            return objectStorageService.store(ObjectStoreCommand.reportPdf(
                    "REPORT",
                    medicalCase.orgId(),
                    medicalCase.caseNo(),
                    reportTypeCode,
                    versionNo,
                    reportNo,
                    fileName,
                    "application/pdf",
                    new ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    md5(pdfBytes)));
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Store report file failed");
        }
    }

    private StoredObject copyReportToExportBucket(ReportRecordModel report,
                                                  ReportAttachmentModel attachment,
                                                  Long operatorId,
                                                  Long exportLogId) {
        try {
            StoredObjectResource source = objectStorageService.load(
                    attachment.bucketName(),
                    attachment.objectKey(),
                    attachment.originalName(),
                    attachment.contentType());
            byte[] bytes;
            try (InputStream inputStream = source.resource().getInputStream()) {
                bytes = inputStream.readAllBytes();
            }
            String fileName = "%s.pdf".formatted(report.reportNo());
            return objectStorageService.store(ObjectStoreCommand.exportFile(
                    "EXPORT",
                    report.orgId(),
                    operatorId,
                    exportLogId,
                    report.reportNo(),
                    fileName,
                    source.contentType(),
                    new ByteArrayInputStream(bytes),
                    bytes.length,
                    md5(bytes)));
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Export report file failed");
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

    private String reportAssetType(String reportTypeCode) {
        return "PATIENT".equalsIgnoreCase(reportTypeCode) ? "PATIENT_REPORT" : "DOCTOR_REPORT";
    }

    private String patientExplanation(String reportTypeCode, String reportNo, ReportRenderDataModel renderData) {
        if (!"PATIENT".equalsIgnoreCase(reportTypeCode) || ragAppService == null) {
            return null;
        }
        return trimToNull(ragAppService.generatePatientReportExplanation(reportNo, renderData));
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
