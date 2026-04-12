package com.cariesguard.patient.interfaces.vo;

import java.util.List;

public record PatientDetailVO(
        Long patientId,
        String patientNo,
        String patientNameMasked,
        String genderCode,
        Integer age,
        String sourceCode,
        List<PatientGuardianVO> guardianList,
        PatientProfileVO currentProfile) {
}
