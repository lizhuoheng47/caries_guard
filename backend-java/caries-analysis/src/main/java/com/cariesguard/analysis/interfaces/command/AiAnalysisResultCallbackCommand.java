package com.cariesguard.analysis.interfaces.command;

import com.cariesguard.analysis.interfaces.dto.AiVisualAssetDTO;
import com.cariesguard.analysis.interfaces.dto.RiskAssessmentDTO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public record AiAnalysisResultCallbackCommand(
        @NotBlank String taskNo,
        @NotBlank String taskStatusCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String modelVersion,
        @Valid Summary summary,
        JsonNode rawResultJson,
        @Valid List<AiVisualAssetDTO> visualAssets,
        @Valid RiskAssessmentDTO riskAssessment,
        String riskLevel,
        JsonNode riskFactors,
        String reviewReason,
        String knowledgeVersion,
        JsonNode evidenceRefs,
        String doctorReviewRequiredReason,
        String errorCode,
        String errorMessage,
        String traceId,
        Long inferenceMillis,
        String gradingLabel,
        Double confidenceScore,
        Boolean needsReview,
        Double uncertaintyScore) {

    public record Summary(
            String overallHighestSeverity,
            Double uncertaintyScore,
            String reviewSuggestedFlag,
            Integer teethCount) {
    }
}
