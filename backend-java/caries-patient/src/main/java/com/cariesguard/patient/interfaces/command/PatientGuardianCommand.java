package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record PatientGuardianCommand(
        @NotBlank String guardianName,
        String relationCode,
        String phone,
        String certificateTypeCode,
        String certificateNo,
        String primaryFlag,
        String status,
        String remark) {
}
