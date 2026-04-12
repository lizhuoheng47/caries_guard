package com.cariesguard.report.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportRenderDataModel(
        String caseNo,
        Long caseId,
        Long patientId,
        String reportTypeCode,
        int imageCount,
        String highestSeverity,
        BigDecimal uncertaintyScore,
        String riskLevelCode,
        Integer recommendedCycleDays,
        String reviewSuggestedFlag,
        String latestCorrectionJson,
        String doctorConclusion,
        LocalDateTime generatedAt) {
}

