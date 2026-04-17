package com.cariesguard.report.domain.model;

import java.util.Map;

public record RagPatientExplanationRequestModel(
        String traceId,
        String question,
        String kbCode,
        Integer topK,
        String relatedBizNo,
        String patientUuid,
        Long javaUserId,
        Long orgId,
        Map<String, Object> caseSummary,
        String riskLevelCode) {
}
