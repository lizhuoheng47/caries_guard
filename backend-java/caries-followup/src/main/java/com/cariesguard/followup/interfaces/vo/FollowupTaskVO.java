package com.cariesguard.followup.interfaces.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowupTaskVO(
        Long taskId,
        String taskNo,
        Long planId,
        Long caseId,
        String taskTypeCode,
        String taskStatusCode,
        Long assignedToUserId,
        LocalDate dueDate,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String remark,
        LocalDateTime createdAt) {
}
