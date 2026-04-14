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
        LocalDateTime completedAt,
        String traceId,
        Long inferenceMillis) {

    public AnalysisTaskVO(Long taskId,
                          String taskNo,
                          String taskStatusCode,
                          String taskTypeCode,
                          String modelVersion,
                          String errorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime startedAt,
                          LocalDateTime completedAt) {
        this(taskId, taskNo, taskStatusCode, taskTypeCode, modelVersion, errorMessage,
                createdAt, startedAt, completedAt, null, null);
    }
}
