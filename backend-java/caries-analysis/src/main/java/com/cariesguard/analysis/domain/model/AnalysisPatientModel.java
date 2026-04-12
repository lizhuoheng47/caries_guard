package com.cariesguard.analysis.domain.model;

public record AnalysisPatientModel(
        Long patientId,
        Integer age,
        String genderCode) {
}
