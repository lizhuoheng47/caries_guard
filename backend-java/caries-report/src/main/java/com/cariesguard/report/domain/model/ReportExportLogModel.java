package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportExportLogModel(
        Long exportLogId,
        Long reportId,
        Long attachmentId,
        String exportTypeCode,
        String exportChannelCode,
        Long exportedBy,
        LocalDateTime exportedAt,
        Long orgId) {
}

