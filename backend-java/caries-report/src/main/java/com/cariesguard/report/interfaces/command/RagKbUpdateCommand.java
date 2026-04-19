package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record RagKbUpdateCommand(
        String docTitle,
        String docSourceCode,
        String sourceUri,
        @NotBlank String contentText,
        String changeSummary) {
}
