package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record CaseDiagnosisCreateModel(
        Long diagnosisId,
        Long caseId,
        String diagnosisTypeCode,
        String diagnosisName,
        String severityCode,
        String diagnosisBasis,
        String diagnosisDesc,
        String treatmentAdvice,
        Long reviewDoctorId,
        LocalDateTime reviewTime,
        String finalFlag,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
