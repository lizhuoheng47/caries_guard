package com.cariesguard.analysis.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RiskAssessmentDTO(
        String overallRiskLevelCode,
        JsonNode assessmentReportJson,
        Integer recommendedCycleDays) {
}
