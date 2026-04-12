package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record CreateReportTemplateCommand(
        @NotBlank String templateCode,
        @NotBlank String templateName,
        @NotBlank String reportTypeCode,
        @NotBlank String templateContent,
        String status,
        String remark) {
}

