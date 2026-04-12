package com.cariesguard.analysis.interfaces.vo;

public record AnalysisSummaryVO(
        String overallHighestSeverity,
        Double uncertaintyScore,
        String reviewSuggestedFlag,
        Integer teethCount) {
}
