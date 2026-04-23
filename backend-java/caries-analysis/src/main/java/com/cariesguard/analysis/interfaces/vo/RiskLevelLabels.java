package com.cariesguard.analysis.interfaces.vo;

import java.util.Map;

public final class RiskLevelLabels {

    private RiskLevelLabels() {
    }

    private static final Map<String, String> LABELS = Map.of(
            "HIGH", "高风险",
            "MEDIUM", "中风险",
            "LOW", "低风险",
            "VERY_HIGH", "极高风险",
            "MINIMAL", "极低风险"
    );

    public static String toLabel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(riskLevel.trim().toUpperCase(java.util.Locale.ROOT), riskLevel);
    }
}
