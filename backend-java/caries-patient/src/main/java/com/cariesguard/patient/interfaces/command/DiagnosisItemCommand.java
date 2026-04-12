package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record DiagnosisItemCommand(
        String diagnosisTypeCode,
        @NotBlank String diagnosisName,
        String severityCode,
        String diagnosisBasis,
        String diagnosisDesc,
        String treatmentAdvice,
        String finalFlag,
        String remark) {
}
