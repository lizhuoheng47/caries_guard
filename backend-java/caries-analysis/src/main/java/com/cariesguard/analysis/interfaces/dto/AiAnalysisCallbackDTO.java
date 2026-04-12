package com.cariesguard.analysis.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public record AiAnalysisCallbackDTO(
        @NotBlank String taskNo,
        @NotBlank String taskStatusCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String modelVersion,
        @Valid Summary summary,
        JsonNode rawResultJson,
        @Valid List<AiVisualAssetDTO> visualAssets,
        @Valid RiskAssessmentDTO riskAssessment,
        String errorMessage) {

    public record Summary(
            String overallHighestSeverity,
            Double uncertaintyScore,
            String reviewSuggestedFlag,
            Integer teethCount) {
    }
}
