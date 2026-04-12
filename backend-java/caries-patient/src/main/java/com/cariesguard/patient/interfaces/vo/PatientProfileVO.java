package com.cariesguard.patient.interfaces.vo;

import java.time.LocalDate;

public record PatientProfileVO(
        Integer brushingFreqPerDay,
        String sugarDietLevelCode,
        String fluorideUseFlag,
        String familyCariesHistoryFlag,
        String orthodonticHistoryFlag,
        Integer previousCariesCount,
        Integer lastDentalCheckMonths,
        String oralHygieneLevelCode,
        LocalDate effectiveDate) {
}
