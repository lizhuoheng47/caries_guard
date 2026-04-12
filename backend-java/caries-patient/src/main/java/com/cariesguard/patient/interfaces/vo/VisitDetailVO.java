package com.cariesguard.patient.interfaces.vo;

import java.time.LocalDateTime;

public record VisitDetailVO(
        Long visitId,
        String visitNo,
        Long patientId,
        Long departmentId,
        Long doctorUserId,
        String visitTypeCode,
        LocalDateTime visitDate,
        String complaint,
        String triageLevelCode,
        String sourceChannelCode,
        String status,
        String remark) {
}
