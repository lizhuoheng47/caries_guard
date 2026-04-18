package com.cariesguard.report.interfaces.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportRiskAssessmentVO(
        Long riskAssessmentId,
        String overallRiskLevelCode,
        BigDecimal riskScore,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        String followupSuggestion,
        Boolean reviewSuggested,
        String explanation,
        String fusionVersion,
        List<ReportRiskFactorVO> riskFactors,
        LocalDateTime assessedAt) {

    public ReportRiskAssessmentVO {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }

    public ReportRiskAssessmentVO(Long riskAssessmentId,
                                  String overallRiskLevelCode,
                                  String assessmentReportJson,
                                  Integer recommendedCycleDays,
                                  LocalDateTime assessedAt) {
        this(riskAssessmentId, overallRiskLevelCode, null, assessmentReportJson, recommendedCycleDays,
                null, null, null, null, List.of(), assessedAt);
    }
}
