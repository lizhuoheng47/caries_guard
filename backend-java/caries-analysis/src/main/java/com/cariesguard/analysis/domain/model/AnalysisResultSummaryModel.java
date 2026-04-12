package com.cariesguard.analysis.domain.model;

import java.math.BigDecimal;

public record AnalysisResultSummaryModel(
        Long summaryId,
        Long taskId,
        Long caseId,
        String rawResultJson,
        String overallHighestSeverity,
        BigDecimal uncertaintyScore,
        String reviewSuggestedFlag,
        Long orgId,
        Long operatorUserId) {
}
