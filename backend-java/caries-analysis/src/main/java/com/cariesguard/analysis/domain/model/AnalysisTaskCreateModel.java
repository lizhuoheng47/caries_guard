package com.cariesguard.analysis.domain.model;

public record AnalysisTaskCreateModel(
        Long taskId,
        String taskNo,
        Long caseId,
        Long patientId,
        String modelVersion,
        String taskTypeCode,
        String taskStatusCode,
        String requestPayloadJson,
        Long orgId,
        String status,
        Long operatorUserId) {
}
