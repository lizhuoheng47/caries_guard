package com.cariesguard.dashboard.interfaces.vo;

public record RiskLevelDistributionVO(
        long highRiskCount,
        long mediumRiskCount,
        long lowRiskCount) {
}
