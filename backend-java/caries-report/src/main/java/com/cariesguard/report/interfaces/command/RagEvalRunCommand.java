package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotNull;

public record RagEvalRunCommand(@NotNull Long datasetId) {
}
