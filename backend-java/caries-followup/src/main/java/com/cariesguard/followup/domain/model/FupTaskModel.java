package com.cariesguard.followup.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FupTaskModel(
        Long taskId,
        String taskNo,
        Long planId,
        Long caseId,
        Long patientId,
        String taskTypeCode,
        String taskStatusCode,
        Long assignedToUserId,
        LocalDate dueDate,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Long orgId,
        String remark,
        LocalDateTime createdAt) {
}