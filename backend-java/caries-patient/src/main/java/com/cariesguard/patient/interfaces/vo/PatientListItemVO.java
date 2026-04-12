package com.cariesguard.patient.interfaces.vo;

import java.time.LocalDate;

public record PatientListItemVO(
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
