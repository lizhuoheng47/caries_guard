package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record CaseToothRecordCreateModel(
        Long recordId,
        Long caseId,
        Long sourceImageId,
        String toothCode,
        String toothSurfaceCode,
        String issueTypeCode,
        String severityCode,
        String findingDesc,
        String suggestion,
        Integer sortOrder,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
