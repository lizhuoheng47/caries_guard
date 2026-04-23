package com.cariesguard.dashboard.interfaces.vo;

public record DashboardOverviewVO(
        long patientCount,
        long caseCount,
        long analyzedCaseCount,
        long generatedReportCount,
        long followupRequiredCaseCount,
        long closedCaseCount,
        long todayAnalysisTaskCount,
        java.math.BigDecimal averageInferenceMillis,
        java.math.BigDecimal highUncertaintyRate,
        java.math.BigDecimal reviewPassRate,
        java.math.BigDecimal doctorAdoptionRate) {
}
