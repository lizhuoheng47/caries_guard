package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisTaskStatusUpdateModel(
        String taskNo,
        String taskStatusCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}
