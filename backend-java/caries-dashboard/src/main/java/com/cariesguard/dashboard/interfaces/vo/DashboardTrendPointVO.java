package com.cariesguard.dashboard.interfaces.vo;

import java.time.LocalDate;

public record DashboardTrendPointVO(
        LocalDate date,
        long newCaseCount,
        long analysisCompletedCount,
        long reportGeneratedCount,
        long followupTriggeredCount) {
}
