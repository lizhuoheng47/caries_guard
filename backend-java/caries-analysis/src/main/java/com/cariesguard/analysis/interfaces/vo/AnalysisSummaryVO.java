package com.cariesguard.analysis.interfaces.vo;

public record AnalysisSummaryVO(
        String overallHighestSeverity,
        Double uncertaintyScore,
        String reviewSuggestedFlag,
        Integer lesionCount,
        Integer abnormalToothCount,
        Integer summaryVersionNo,
        Integer teethCount) {

    public AnalysisSummaryVO(String overallHighestSeverity,
                             Double uncertaintyScore,
                             String reviewSuggestedFlag,
                             Integer teethCount) {
        this(overallHighestSeverity, uncertaintyScore, reviewSuggestedFlag, null, null, null, teethCount);
    }
}
