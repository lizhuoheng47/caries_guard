package com.cariesguard.report.interfaces.vo;

public record ReportExportResultVO(
        Long reportId,
        boolean exported,
        Long exportLogId) {
}

