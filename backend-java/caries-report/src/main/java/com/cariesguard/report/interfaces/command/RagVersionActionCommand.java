package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record RagVersionActionCommand(
        @NotBlank String versionNo,
        String comment) {
}
