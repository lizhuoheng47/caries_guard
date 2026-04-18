package com.cariesguard.report.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportRiskAssessmentModel(
        Long riskAssessmentId,
        String overallRiskLevelCode,
        BigDecimal riskScore,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        String followupSuggestion,
        Boolean reviewSuggested,
        String explanation,
        String fusionVersion,
        List<RiskFactorModel> riskFactors,
        LocalDateTime assessedAt) {

    public ReportRiskAssessmentModel {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }

    public ReportRiskAssessmentModel(Long riskAssessmentId,
                                     String overallRiskLevelCode,
                                     String assessmentReportJson,
                                     Integer recommendedCycleDays,
                                     LocalDateTime assessedAt) {
        this(riskAssessmentId, overallRiskLevelCode, null, assessmentReportJson, recommendedCycleDays,
                null, null, null, null, List.of(), assessedAt);
    }

    public record RiskFactorModel(
            String code,
            BigDecimal weight,
            String source,
            String evidence) {
    }
}
