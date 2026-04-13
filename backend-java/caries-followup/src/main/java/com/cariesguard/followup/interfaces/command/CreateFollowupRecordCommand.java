package com.cariesguard.followup.interfaces.command;

import jakarta.validation.constraints.NotNull;

public record CreateFollowupRecordCommand(
        @NotNull Long taskId,
        String followupMethodCode,
        String contactResultCode,
        Boolean followNext,
        Integer nextIntervalDays,
        String outcomeSummary,
        String doctorNotes,
        String remark) {
}
