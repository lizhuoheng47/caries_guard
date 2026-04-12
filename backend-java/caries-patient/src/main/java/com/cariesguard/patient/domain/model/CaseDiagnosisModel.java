package com.cariesguard.patient.domain.model;

public record CaseDiagnosisModel(
        String diagnosisName,
        String severityCode,
        String finalFlag) {
}
