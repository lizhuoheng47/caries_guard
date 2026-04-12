package com.cariesguard.patient.domain.model;

public record CaseDiagnosisModel(
        String diagnosisTypeCode,
        String diagnosisName,
        String diagnosisBasis,
        String diagnosisDesc,
        String treatmentAdvice,
        String severityCode,
        String finalFlag) {
}
