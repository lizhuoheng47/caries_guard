package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

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
        JsonNode riskFactors,
        JsonNode evidenceRefs,
        String gradingLabel,
        Double confidenceScore,
        Boolean needsReview,
        String followUpRecommendation,
        String reviewReasonLabel,
        Map<String, List<EvidenceRefItemVO>> classifiedEvidenceRefs,
        List<AnalysisCitationVO> citations,
        JsonNode rawResultJson,
        Boolean rawResultJsonCollapsed,
        String riskLevelLabel) {
}
