package com.cariesguard.report.interfaces.vo;

public record ReportExportResultVO(
        Long reportId,
        boolean exported,
        Long exportLogId,
        Long attachmentId,
        String downloadUrl,
        Long expireAt) {

    public ReportExportResultVO(Long reportId, boolean exported, Long exportLogId) {
        this(reportId, exported, exportLogId, null, null, null);
    }
}
