package com.cariesguard.analysis.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record RiskAssessmentDTO(
        String overallRiskLevelCode,
        JsonNode assessmentReportJson,
        Integer recommendedCycleDays,
        BigDecimal riskScore) {
}
