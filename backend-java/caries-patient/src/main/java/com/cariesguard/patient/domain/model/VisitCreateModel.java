package com.cariesguard.patient.domain.model;

import java.time.LocalDateTime;

public record VisitCreateModel(
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
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
