package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RagAskCommand(
        @NotBlank String question,
        String scene,
        String kbCode,
        @Min(1) @Max(20) Integer topK,
        String relatedBizNo,
        String patientUuid,
        Map<String, Object> caseContext) {
}
