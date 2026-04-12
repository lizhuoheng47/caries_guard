package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportRecordModel(
        Long reportId,
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
}

