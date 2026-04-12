package com.cariesguard.patient.domain.model;

import java.util.List;

public record PatientDetailModel(
        Long patientId,
        String patientNo,
        String patientNameMasked,
        String genderCode,
        Integer age,
        String sourceCode,
        Long orgId,
        List<PatientGuardianModel> guardianList,
        PatientProfileSnapshotModel currentProfile) {
}
