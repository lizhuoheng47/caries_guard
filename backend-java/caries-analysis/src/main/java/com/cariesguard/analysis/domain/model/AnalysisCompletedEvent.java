package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisCompletedEvent(
        Long taskId,
        String taskNo,
        Long caseId,
        String modelVersion,
        LocalDateTime completedAt) {
}
