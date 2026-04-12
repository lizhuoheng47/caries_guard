package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCaseCommand(
        @NotNull Long visitId,
        @NotNull Long patientId,
        String caseTypeCode,
        String caseTitle,
        String chiefComplaint,
        String priorityCode,
        String clinicalNotes,
        LocalDate onsetDate,
        String status,
        String remark) {
}
