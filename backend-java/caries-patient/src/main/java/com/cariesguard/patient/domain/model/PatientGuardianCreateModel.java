package com.cariesguard.patient.domain.model;

public record PatientGuardianCreateModel(
        Long guardianId,
        Long patientId,
        String guardianNameEnc,
        String guardianNameHash,
        String guardianNameMasked,
        String relationCode,
        String phoneEnc,
        String phoneHash,
        String phoneMasked,
        String certificateTypeCode,
        String certificateNoEnc,
        String certificateNoHash,
        String certificateNoMasked,
        String primaryFlag,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
