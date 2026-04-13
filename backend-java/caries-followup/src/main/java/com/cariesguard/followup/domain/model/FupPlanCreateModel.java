package com.cariesguard.followup.domain.model;

import java.time.LocalDate;

public record FupPlanCreateModel(
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
        Long orgId,
        String remark,
        Long operatorUserId) {
}