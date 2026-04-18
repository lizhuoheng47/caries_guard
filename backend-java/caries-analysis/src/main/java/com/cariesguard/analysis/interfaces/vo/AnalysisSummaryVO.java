package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalysisSummaryVO(
        String overallHighestSeverity,
        Double uncertaintyScore,
        String reviewSuggestedFlag,
        Integer lesionCount,
        Integer abnormalToothCount,
        Integer summaryVersionNo,
        Integer teethCount,
        String riskLevel,
        String reviewReason,
        String doctorReviewRequiredReason,
        String knowledgeVersion,
        JsonNode riskFactors,
        JsonNode evidenceRefs) {

    public AnalysisSummaryVO(String overallHighestSeverity,
                             Double uncertaintyScore,
                             String reviewSuggestedFlag,
                             Integer teethCount) {
        this(overallHighestSeverity, uncertaintyScore, reviewSuggestedFlag, null, null, null, teethCount,
                null, null, null, null, null, null);
    }
}
