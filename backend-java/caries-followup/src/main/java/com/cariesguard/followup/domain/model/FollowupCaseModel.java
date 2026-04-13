package com.cariesguard.followup.domain.model;

public record FollowupCaseModel(
        Long caseId,
        Long patientId,
        String caseStatusCode,
        Long orgId) {
}
