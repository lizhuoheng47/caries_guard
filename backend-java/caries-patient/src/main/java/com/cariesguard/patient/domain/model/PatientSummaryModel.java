package com.cariesguard.patient.domain.model;

import java.time.LocalDate;

public record PatientSummaryModel(
        Long patientId,
        String patientNo,
        String patientNameMasked,
        String genderCode,
        Integer age,
        String phoneMasked,
        String sourceCode,
        LocalDate firstVisitDate,
        String status) {
}
