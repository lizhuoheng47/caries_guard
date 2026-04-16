package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisTaskStatusUpdateModel(
        String taskNo,
        String taskStatusCode,
        String callbackPayloadJson,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String traceId,
        Long inferenceMillis,
        String modelVersion) {

    public AnalysisTaskStatusUpdateModel(String taskNo,
                                         String taskStatusCode,
                                         String errorMessage,
                                         LocalDateTime startedAt,
                                         LocalDateTime completedAt,
                                         String traceId,
                                         Long inferenceMillis,
                                         String modelVersion) {
        this(taskNo, taskStatusCode, null, null, errorMessage, startedAt, completedAt, traceId, inferenceMillis, modelVersion);
    }
}
