package com.cariesguard.followup.domain.model;

import java.time.LocalDateTime;

public record FupRecordCreateModel(
        Long recordId,
        String recordNo,
        Long taskId,
        Long planId,
        Long caseId,
        Long patientId,
        String followupMethodCode,
        String contactResultCode,
        String followNextFlag,
        Integer nextIntervalDays,
        String outcomeSummary,
        String doctorNotes,
        LocalDateTime recordedAt,
        Long orgId,
        String remark,
        Long operatorUserId) {
}