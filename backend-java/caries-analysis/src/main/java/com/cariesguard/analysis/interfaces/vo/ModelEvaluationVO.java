package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record ModelEvaluationVO(
        Long evaluationId,
        Long modelVersionId,
        Long datasetSnapshotId,
        String evalTypeCode,
        JsonNode metricJson,
        JsonNode errorCaseJson,
        String evidenceAttachmentKey,
        LocalDateTime evaluatedAt,
        Long evaluatorUserId,
        String status) {
}
