package com.cariesguard.followup.interfaces.vo;

public record FollowupTriggerResultVO(
        boolean triggered,
        Long planId,
        String planNo,
        Long taskId,
        String taskNo,
        String skipReason) {

    public static FollowupTriggerResultVO triggered(Long planId, String planNo, Long taskId, String taskNo) {
        return new FollowupTriggerResultVO(true, planId, planNo, taskId, taskNo, null);
    }

    public static FollowupTriggerResultVO skipped(String reason) {
        return new FollowupTriggerResultVO(false, null, null, null, null, reason);
    }
}
