package com.cariesguard.patient.interfaces.vo;

public record CaseDiagnosisVO(
        String diagnosisName,
        String severityCode,
        String finalFlag) {
}
