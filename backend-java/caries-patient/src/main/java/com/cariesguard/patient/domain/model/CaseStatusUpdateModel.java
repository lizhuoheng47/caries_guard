package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record CaseStatusUpdateModel(
        Long caseId,
        String caseStatusCode,
        String reportReadyFlag,
        String followupRequiredFlag,
        LocalDateTime closedAt,
        Long operatorUserId) {
}
