package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record GenerateReportCommand(
        @NotBlank String reportTypeCode,
        String doctorConclusion,
        String remark) {
}

