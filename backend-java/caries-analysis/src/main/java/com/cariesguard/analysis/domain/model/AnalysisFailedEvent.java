package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record AnalysisFailedEvent(
        Long taskId,
        String taskNo,
        Long caseId,
        String errorMessage,
        LocalDateTime failedAt) {
}
