package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExportCorrectionFeedbackCommand(
        @Min(1) @Max(500) Integer limit) {
}
