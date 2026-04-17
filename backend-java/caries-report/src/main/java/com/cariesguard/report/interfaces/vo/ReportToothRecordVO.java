package com.cariesguard.report.interfaces.vo;

public record ReportToothRecordVO(
        Long toothRecordId,
        Long sourceImageId,
        String toothCode,
        String toothSurfaceCode,
        String issueTypeCode,
        String severityCode,
        String findingDesc,
        String suggestion,
        Integer sortOrder) {
}
