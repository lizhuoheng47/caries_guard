package com.cariesguard.report.domain.model;

public record ReportToothRecordModel(
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
