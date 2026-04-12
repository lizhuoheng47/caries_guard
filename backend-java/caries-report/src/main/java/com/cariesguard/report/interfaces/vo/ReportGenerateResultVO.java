package com.cariesguard.report.interfaces.vo;

public record ReportGenerateResultVO(
        Long reportId,
        String reportNo,
        String reportTypeCode,
        Integer versionNo,
        String reportStatusCode) {
}

