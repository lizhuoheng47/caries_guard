package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportRiskAssessmentModel(
        Long riskAssessmentId,
        String overallRiskLevelCode,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        LocalDateTime assessedAt) {
}

