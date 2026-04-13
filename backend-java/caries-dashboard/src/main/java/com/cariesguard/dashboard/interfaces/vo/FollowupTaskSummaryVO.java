package com.cariesguard.dashboard.interfaces.vo;

import java.math.BigDecimal;

public record FollowupTaskSummaryVO(
        long todoCount,
        long inProgressCount,
        long doneCount,
        long overdueCount,
        BigDecimal completionRate,
        BigDecimal overdueRate) {
}
