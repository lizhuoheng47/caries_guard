package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;

public record ReportDetailVO(
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
        LocalDateTime createdAt) {
}

