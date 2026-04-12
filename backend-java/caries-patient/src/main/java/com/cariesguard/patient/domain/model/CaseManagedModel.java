package com.cariesguard.patient.domain.model;

public record CaseManagedModel(
        Long caseId,
        String caseStatusCode,
        String reportReadyFlag,
        String followupRequiredFlag,
        Long orgId) {
}
