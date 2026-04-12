package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record RiskAssessmentCreateModel(
        Long recordId,
        Long caseId,
        Long patientId,
        String overallRiskLevelCode,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        LocalDateTime assessedAt,
        Long orgId,
        Long operatorUserId) {
}
