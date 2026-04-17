package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;

public record PatientExplanationCommand(
        String question,
        String kbCode,
        @Min(1) @Max(20) Integer topK,
        String relatedBizNo,
        String patientUuid,
        Map<String, Object> caseSummary,
        String riskLevelCode) {
}
