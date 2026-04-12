package com.cariesguard.patient.domain.model;

public record VisitOwnedModel(
        Long visitId,
        Long patientId,
        Long doctorUserId,
        Long orgId) {
}
