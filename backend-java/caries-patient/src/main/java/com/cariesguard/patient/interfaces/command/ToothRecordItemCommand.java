package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record ToothRecordItemCommand(
        Long sourceImageId,
        @NotBlank String toothCode,
        String toothSurfaceCode,
        String issueTypeCode,
        String severityCode,
        String findingDesc,
        String suggestion,
        Integer sortOrder,
        String remark) {
}
