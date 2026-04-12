package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;

public record ReportListItemVO(
        Long reportId,
        String reportNo,
        String reportTypeCode,
        Integer versionNo,
        String reportStatusCode,
        Long attachmentId,
        LocalDateTime generatedAt,
        LocalDateTime createdAt) {
}

