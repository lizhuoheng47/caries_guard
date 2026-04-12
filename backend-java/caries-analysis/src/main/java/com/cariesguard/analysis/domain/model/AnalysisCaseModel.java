package com.cariesguard.analysis.domain.model;

public record AnalysisCaseModel(
        Long caseId,
        String caseNo,
        Long patientId,
        Long orgId,
        String caseStatusCode) {
}
