package com.cariesguard.analysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskAssessmentCreateModel(
        Long recordId,
        Long caseId,
        Long patientId,
        Long taskId,
        String overallRiskLevelCode,
        BigDecimal riskScore,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        Integer versionNo,
        LocalDateTime assessedAt,
        Long orgId,
        Long operatorUserId) {

    public RiskAssessmentCreateModel(Long recordId,
                                     Long caseId,
                                     Long patientId,
                                     String overallRiskLevelCode,
                                     String assessmentReportJson,
                                     Integer recommendedCycleDays,
                                     LocalDateTime assessedAt,
                                     Long orgId,
                                     Long operatorUserId) {
        this(recordId, caseId, patientId, null, overallRiskLevelCode, null, assessmentReportJson,
                recommendedCycleDays, 1, assessedAt, orgId, operatorUserId);
    }
}
