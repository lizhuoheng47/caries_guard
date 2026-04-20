package com.cariesguard.analysis.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewQueueItemVO(
        String taskNo,
        Long caseId,
        String caseNo,
        String taskStatusCode,
        String reviewStatusCode,
        String secondOpinionStatusCode,
        String gradingLabel,
        Double uncertaintyScore,
        Boolean needsReview,
        String riskLevelCode,
        List<String> reviewReasonCodes,
        List<String> reviewReasonLabels,
        String primaryToothCode,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}

