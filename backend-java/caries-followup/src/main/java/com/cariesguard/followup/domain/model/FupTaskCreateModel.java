package com.cariesguard.followup.domain.model;

import java.time.LocalDate;

public record FupTaskCreateModel(
        Long taskId,
        String taskNo,
        Long planId,
        Long caseId,
        Long patientId,
        String taskTypeCode,
        String taskStatusCode,
        Long assignedToUserId,
        LocalDate dueDate,
        Long orgId,
        String remark,
        Long operatorUserId) {
}