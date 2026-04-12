package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record CaseStatusLogCreateModel(
        Long logId,
        Long caseId,
        String fromStatusCode,
        String toStatusCode,
        Long changedBy,
        String changeReasonCode,
        String changeReason,
        LocalDateTime changedAt,
        Long orgId) {
}
