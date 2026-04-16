package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportGenerateModel(
        Long reportId,
        String reportNo,
        Long caseId,
        Long patientId,
        Long sourceSummaryId,
        Long sourceRiskAssessmentId,
        Long sourceCorrectionId,
        String reportTypeCode,
        Integer versionNo,
        String reportStatusCode,
        String summaryText,
        LocalDateTime generatedAt,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {

    public ReportGenerateModel(Long reportId,
                               String reportNo,
                               Long caseId,
                               Long patientId,
                               String reportTypeCode,
                               Integer versionNo,
                               String reportStatusCode,
                               String summaryText,
                               LocalDateTime generatedAt,
                               Long orgId,
                               String status,
                               String remark,
                               Long operatorUserId) {
        this(reportId, reportNo, caseId, patientId, null, null, null, reportTypeCode, versionNo,
                reportStatusCode, summaryText, generatedAt, orgId, status, remark, operatorUserId);
    }
}
