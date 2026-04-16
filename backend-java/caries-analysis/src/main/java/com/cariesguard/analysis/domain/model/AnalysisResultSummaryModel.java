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
        Integer lesionCount,
        Integer abnormalToothCount,
        Integer summaryVersionNo,
        Long orgId,
        Long operatorUserId) {

    public AnalysisResultSummaryModel(Long summaryId,
                                      Long taskId,
                                      Long caseId,
                                      String rawResultJson,
                                      String overallHighestSeverity,
                                      BigDecimal uncertaintyScore,
                                      String reviewSuggestedFlag,
                                      Long orgId,
                                      Long operatorUserId) {
        this(summaryId, taskId, caseId, rawResultJson, overallHighestSeverity, uncertaintyScore,
                reviewSuggestedFlag, null, null, 1, orgId, operatorUserId);
    }
}
