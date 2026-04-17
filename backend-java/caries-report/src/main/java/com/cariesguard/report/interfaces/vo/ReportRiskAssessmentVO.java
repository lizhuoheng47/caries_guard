package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;

public record ReportRiskAssessmentVO(
        Long riskAssessmentId,
        String overallRiskLevelCode,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        LocalDateTime assessedAt) {
}
