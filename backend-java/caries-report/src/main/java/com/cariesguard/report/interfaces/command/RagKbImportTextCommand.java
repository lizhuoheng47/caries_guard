package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record RagKbImportTextCommand(
        String kbCode,
        String kbName,
        String kbTypeCode,
        String docNo,
        @NotBlank String docTitle,
        String docSourceCode,
        String sourceUri,
        String docVersion,
        @NotBlank String contentText,
        String reviewStatusCode) {
}
