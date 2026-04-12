package com.cariesguard.patient.interfaces.vo;

public record CaseDiagnosisVO(
        String diagnosisTypeCode,
        String diagnosisName,
        String diagnosisBasis,
        String diagnosisDesc,
        String treatmentAdvice,
        String severityCode,
        String finalFlag) {
}
