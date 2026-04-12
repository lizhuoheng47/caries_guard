package com.cariesguard.patient.interfaces.vo;

public record CaseToothRecordVO(
        Long sourceImageId,
        String toothCode,
        String toothSurfaceCode,
        String issueTypeCode,
        String severityCode,
        String findingDesc,
        String suggestion,
        Integer sortOrder) {
}
