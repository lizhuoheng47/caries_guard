package com.cariesguard.followup.interfaces.vo;

import java.time.LocalDateTime;

public record FollowupRecordVO(
        Long recordId,
        String recordNo,
        Long taskId,
        Long planId,
        Long caseId,
        String followupMethodCode,
        String contactResultCode,
        boolean followNext,
        Integer nextIntervalDays,
        String outcomeSummary,
        String doctorNotes,
        LocalDateTime recordedAt,
        LocalDateTime createdAt) {
}
