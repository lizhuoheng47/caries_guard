package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record CaseSummaryModel(
        Long caseId,
        String caseNo,
        Long patientId,
        Long visitId,
        String caseTitle,
        String caseStatusCode,
        String priorityCode,
        Long attendingDoctorId,
        String reportReadyFlag,
        String followupRequiredFlag,
        LocalDateTime createdAt) {
}
