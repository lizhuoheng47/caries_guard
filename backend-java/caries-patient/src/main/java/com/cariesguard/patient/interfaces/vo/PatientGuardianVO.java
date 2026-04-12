package com.cariesguard.patient.interfaces.vo;

public record PatientGuardianVO(
        String guardianNameMasked,
        String relationCode,
        String phoneMasked,
        String primaryFlag) {
}
