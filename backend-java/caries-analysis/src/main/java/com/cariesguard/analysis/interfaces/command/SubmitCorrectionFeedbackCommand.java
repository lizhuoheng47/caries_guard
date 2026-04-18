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
        JsonNode correctedTruthJson,
        String originalAiGrade,
        String doctorCorrectedGrade,
        Double originalUncertainty,
        Boolean acceptedAiConclusion,
        String correctionReason,
        Boolean trainingCandidate) {

    public SubmitCorrectionFeedbackCommand(Long caseId,
                                           Long diagnosisId,
                                           Long sourceImageId,
                                           String feedbackTypeCode,
                                           JsonNode originalInferenceJson,
                                           JsonNode correctedTruthJson) {
        this(caseId, diagnosisId, sourceImageId, feedbackTypeCode, originalInferenceJson, correctedTruthJson,
                null, null, null, null, null, null);
    }
}
