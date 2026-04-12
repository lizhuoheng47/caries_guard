package com.cariesguard.patient.domain.model;

import java.time.LocalDate;
import java.util.List;

public record PatientUpdateModel(
        Long patientId,
        String patientNameEnc,
        String patientNameHash,
        String patientNameMasked,
        String genderCode,
        String birthDateEnc,
        String birthDateHash,
        String birthDateMasked,
        Integer age,
        String phoneEnc,
        String phoneHash,
        String phoneMasked,
        String idCardEnc,
        String idCardHash,
        String idCardMasked,
        String sourceCode,
        LocalDate firstVisitDate,
        String privacyLevelCode,
        String status,
        String remark,
        Long operatorUserId,
        List<PatientGuardianCreateModel> guardians) {
}
