package com.cariesguard.patient.interfaces.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePatientCommand(
        @NotBlank String patientName,
        String genderCode,
        LocalDate birthDate,
        String phone,
        String idCardNo,
        String sourceCode,
        LocalDate firstVisitDate,
        String privacyLevelCode,
        String status,
        String remark,
        @Valid PatientGuardianCommand guardian) {
}
