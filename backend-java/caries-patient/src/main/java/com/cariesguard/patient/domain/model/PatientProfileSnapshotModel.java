package com.cariesguard.patient.domain.model;

import java.time.LocalDate;

public record PatientProfileSnapshotModel(
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
