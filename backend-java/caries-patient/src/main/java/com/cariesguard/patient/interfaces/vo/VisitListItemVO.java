package com.cariesguard.patient.interfaces.vo;

import java.time.LocalDateTime;

public record VisitListItemVO(
        Long visitId,
        String visitNo,
        Long patientId,
        Long doctorUserId,
        String visitTypeCode,
        LocalDateTime visitDate,
        String triageLevelCode,
        String status) {
}
