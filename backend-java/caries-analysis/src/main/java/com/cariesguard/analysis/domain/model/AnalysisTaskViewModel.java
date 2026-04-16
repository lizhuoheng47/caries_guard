package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisTaskViewModel(
        Long taskId,
        String taskNo,
        Long caseId,
        Long patientId,
        String requestBatchNo,
        String modelVersion,
        String taskTypeCode,
        String taskStatusCode,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Long orgId,
        Long retryFromTaskId,
        String traceId,
        Long inferenceMillis) {

    public AnalysisTaskViewModel(Long taskId,
                                 String taskNo,
                                 Long caseId,
                                 Long patientId,
                                 String modelVersion,
                                 String taskTypeCode,
                                 String taskStatusCode,
                                 String errorMessage,
                                 LocalDateTime createdAt,
                                 LocalDateTime startedAt,
                                 LocalDateTime completedAt,
                                 Long orgId,
                                 Long retryFromTaskId,
                                 String traceId,
                                 Long inferenceMillis) {
        this(taskId, taskNo, caseId, patientId, null, modelVersion, taskTypeCode, taskStatusCode, null,
                errorMessage, createdAt, startedAt, completedAt, orgId, retryFromTaskId, traceId, inferenceMillis);
    }

    public AnalysisTaskViewModel(Long taskId,
                                 String taskNo,
                                 Long caseId,
                                 Long patientId,
                                 String modelVersion,
                                 String taskTypeCode,
                                 String taskStatusCode,
                                 String errorMessage,
                                 LocalDateTime createdAt,
                                 LocalDateTime startedAt,
                                 LocalDateTime completedAt,
                                 Long orgId,
                                 Long retryFromTaskId) {
        this(taskId, taskNo, caseId, patientId, null, modelVersion, taskTypeCode, taskStatusCode, null,
                errorMessage, createdAt, startedAt, completedAt, orgId, retryFromTaskId, null, null);
    }
}
