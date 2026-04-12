package com.cariesguard.analysis.domain.model;

public record AnalysisRequestedEvent(
        Long taskId,
        String taskNo,
        String taskStatusCode,
        String payloadJson) {
}
