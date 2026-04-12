package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record VisitSummaryModel(
        Long visitId,
        String visitNo,
        Long patientId,
        Long doctorUserId,
        String visitTypeCode,
        LocalDateTime visitDate,
        String triageLevelCode,
        String status) {
}
