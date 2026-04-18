package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReviewCorrectionFeedbackCommand(
        @NotEmpty @Size(max = 500) List<Long> feedbackIds,
        @NotBlank String reviewStatusCode,
        Boolean trainingCandidate) {
}
