package com.cariesguard.report.domain.model;

import java.math.BigDecimal;

public record ReportAnalysisSummaryModel(
        Long summaryId,
        String rawResultJson,
        String overallHighestSeverity,
        BigDecimal uncertaintyScore,
        String reviewSuggestedFlag) {
}

