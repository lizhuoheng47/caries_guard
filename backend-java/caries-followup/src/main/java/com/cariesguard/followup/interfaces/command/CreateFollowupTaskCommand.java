package com.cariesguard.followup.interfaces.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateFollowupTaskCommand(
        @NotNull Long planId,
        String taskTypeCode,
        Long assignedToUserId,
        LocalDate dueDate,
        String remark) {
}
