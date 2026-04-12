package com.cariesguard.analysis.interfaces.vo;

import java.time.LocalDateTime;

public record AnalysisTaskVO(
        Long taskId,
        String taskNo,
        String taskStatusCode,
        String taskTypeCode,
        String modelVersion,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}
