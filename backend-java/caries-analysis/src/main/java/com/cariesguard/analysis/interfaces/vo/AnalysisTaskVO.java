package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record AnalysisTaskVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long taskId,
        String taskNo,
        String taskStatusCode,
        String taskTypeCode,
        String modelVersion,
        String errorCode,
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
                          LocalDateTime completedAt,
                          String traceId,
                          Long inferenceMillis) {
        this(taskId, taskNo, taskStatusCode, taskTypeCode, modelVersion, null, errorMessage,
                createdAt, startedAt, completedAt, traceId, inferenceMillis);
    }

    public AnalysisTaskVO(Long taskId,
                          String taskNo,
                          String taskStatusCode,
                          String taskTypeCode,
                          String modelVersion,
                          String errorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime startedAt,
                          LocalDateTime completedAt) {
        this(taskId, taskNo, taskStatusCode, taskTypeCode, modelVersion, null, errorMessage,
                createdAt, startedAt, completedAt, null, null);
    }
}
