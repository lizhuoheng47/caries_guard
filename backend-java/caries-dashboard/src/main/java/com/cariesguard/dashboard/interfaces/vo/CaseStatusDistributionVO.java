package com.cariesguard.dashboard.interfaces.vo;

public record CaseStatusDistributionVO(
        long createdCount,
        long qcPendingCount,
        long analyzingCount,
        long reviewPendingCount,
        long reportReadyCount,
        long followupRequiredCount,
        long closedCount,
        long cancelledCount) {
}
