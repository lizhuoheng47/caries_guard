package com.cariesguard.followup.interfaces.command;

public record CreateFollowupPlanCommand(
        String planTypeCode,
        Integer intervalDays,
        Long ownerUserId,
        String remark) {
}
