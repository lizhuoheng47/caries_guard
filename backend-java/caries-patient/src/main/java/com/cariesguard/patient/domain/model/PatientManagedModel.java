package com.cariesguard.patient.domain.model;

public record PatientManagedModel(
        Long patientId,
        String patientNo,
        String idCardHash,
        Long orgId) {
}
