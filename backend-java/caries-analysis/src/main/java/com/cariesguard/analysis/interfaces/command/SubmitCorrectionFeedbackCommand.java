package com.cariesguard.analysis.interfaces.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitCorrectionFeedbackCommand(
        @NotNull Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        @NotBlank String feedbackTypeCode,
        JsonNode originalInferenceJson,
        JsonNode correctedTruthJson) {
}
