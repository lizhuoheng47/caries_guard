package com.cariesguard.followup.interfaces.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowupPlanVO(
        Long planId,
        String planNo,
        Long caseId,
        Long patientId,
        String planTypeCode,
        String planStatusCode,
        LocalDate nextFollowupDate,
        Integer intervalDays,
        Long ownerUserId,
        String triggerSourceCode,
        Long triggerRefId,
        String remark,
        LocalDateTime createdAt) {
}
