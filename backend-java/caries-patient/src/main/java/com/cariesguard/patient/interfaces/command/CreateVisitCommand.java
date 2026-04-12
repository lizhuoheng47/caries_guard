package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateVisitCommand(
        @NotNull Long patientId,
        Long departmentId,
        Long doctorUserId,
        String visitTypeCode,
        @NotNull LocalDateTime visitDate,
        String complaint,
        String triageLevelCode,
        String sourceChannelCode,
        String status,
        String remark) {
}
