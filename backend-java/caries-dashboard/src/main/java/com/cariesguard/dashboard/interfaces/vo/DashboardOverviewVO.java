package com.cariesguard.dashboard.interfaces.vo;

public record DashboardOverviewVO(
        long patientCount,
        long caseCount,
        long analyzedCaseCount,
        long generatedReportCount,
        long followupRequiredCaseCount,
        long closedCaseCount) {
}
