package com.cariesguard.analysis.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisTaskDetailVO(
        Long taskId,
        String taskNo,
        Long caseId,
        String taskStatusCode,
        String taskTypeCode,
        String modelVersion,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String traceId,
        Long inferenceMillis,
        AnalysisSummaryVO summary,
        List<AnalysisVisualAssetVO> visualAssets) {
}
