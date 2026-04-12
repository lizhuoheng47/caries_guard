package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportTemplateModel(
        Long templateId,
        String templateCode,
        String templateName,
        String reportTypeCode,
        String templateContent,
        Integer versionNo,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

