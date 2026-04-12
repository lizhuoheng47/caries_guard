package com.cariesguard.patient.interfaces.vo;

import java.time.LocalDateTime;

public record CaseListItemVO(
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
