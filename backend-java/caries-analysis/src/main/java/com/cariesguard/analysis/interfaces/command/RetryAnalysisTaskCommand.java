package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotNull;

public record RetryAnalysisTaskCommand(
        @NotNull Long taskId,
        String reasonCode,
        String reasonRemark) {
}
