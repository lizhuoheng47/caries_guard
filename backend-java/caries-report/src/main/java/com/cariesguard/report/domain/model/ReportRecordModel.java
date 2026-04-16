package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportRecordModel(
        Long reportId,
        String reportNo,
        Long caseId,
        Long patientId,
        Long attachmentId,
        Long sourceSummaryId,
        Long sourceRiskAssessmentId,
        Long sourceCorrectionId,
        String reportTypeCode,
        String reportStatusCode,
        Integer versionNo,
        String summaryText,
        LocalDateTime generatedAt,
        LocalDateTime signedAt,
        Long orgId,
        LocalDateTime createdAt) {

    public ReportRecordModel(Long reportId,
                             String reportNo,
                             Long caseId,
                             Long patientId,
                             Long attachmentId,
                             String reportTypeCode,
                             String reportStatusCode,
                             Integer versionNo,
                             String summaryText,
                             LocalDateTime generatedAt,
                             LocalDateTime signedAt,
                             Long orgId,
                             LocalDateTime createdAt) {
        this(reportId, reportNo, caseId, patientId, attachmentId, null, null, null, reportTypeCode,
                reportStatusCode, versionNo, summaryText, generatedAt, signedAt, orgId, createdAt);
    }
}
