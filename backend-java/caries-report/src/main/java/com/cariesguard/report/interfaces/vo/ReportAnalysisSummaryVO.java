package com.cariesguard.report.interfaces.vo;

import java.math.BigDecimal;

public record ReportAnalysisSummaryVO(
        Long summaryId,
        Long taskId,
        String overallHighestSeverity,
        BigDecimal uncertaintyScore,
        String reviewSuggestedFlag,
        Integer lesionCount,
        Integer abnormalToothCount) {
}
