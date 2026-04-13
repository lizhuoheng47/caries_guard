package com.cariesguard.dashboard.interfaces.vo;

public record BacklogSummaryVO(
        long reviewPendingCaseCount,
        long todoFollowupTaskCount,
        long overdueFollowupTaskCount,
        long highRiskPendingCaseCount) {
}
