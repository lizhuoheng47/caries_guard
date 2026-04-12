package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotNull;

public record CreateAnalysisTaskCommand(
        @NotNull Long caseId,
        Long patientId,
        Boolean forceRetryFlag,
        String taskTypeCode,
        String remark) {
}
