package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisTaskViewModel(
        Long taskId,
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
        Long orgId) {
}
