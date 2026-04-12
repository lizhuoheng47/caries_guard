package com.cariesguard.patient.domain.model;

import java.time.LocalDate;

public record CaseCreateModel(
        Long caseId,
        String caseNo,
        Long visitId,
        Long patientId,
        String caseTitle,
        String caseTypeCode,
        String caseStatusCode,
        String priorityCode,
        String chiefComplaint,
        String clinicalNotes,
        LocalDate onsetDate,
        Long attendingDoctorId,
        Long screenerUserId,
        String reportReadyFlag,
        String followupRequiredFlag,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
