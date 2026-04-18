package com.cariesguard.report.domain.model;

import java.util.Map;

public record RagAskRequestModel(
        String traceId,
        String question,
        String scene,
        String kbCode,
        Integer topK,
        String relatedBizNo,
        String patientUuid,
        Long javaUserId,
        Long orgId,
        Map<String, Object> caseContext) {
}
