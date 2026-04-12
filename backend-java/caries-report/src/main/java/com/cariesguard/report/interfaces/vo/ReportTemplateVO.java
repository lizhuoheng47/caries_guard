package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;

public record ReportTemplateVO(
        Long templateId,
        String templateCode,
        String templateName,
        String reportTypeCode,
        String templateContent,
        Integer versionNo,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

