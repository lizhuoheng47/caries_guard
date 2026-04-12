package com.cariesguard.patient.domain.model;

public record PatientGuardianModel(
        String guardianNameMasked,
        String relationCode,
        String phoneMasked,
        String primaryFlag) {
}
