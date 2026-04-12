package com.cariesguard.patient.domain.model;

public record CaseToothRecordModel(
        Long sourceImageId,
        String toothCode,
        String toothSurfaceCode,
        String issueTypeCode,
        String severityCode,
        String findingDesc,
        String suggestion,
        Integer sortOrder) {
}
