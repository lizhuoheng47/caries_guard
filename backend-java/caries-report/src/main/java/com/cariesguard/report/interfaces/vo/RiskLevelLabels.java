package com.cariesguard.report.interfaces.vo;

import java.util.Map;

/**
 * 风险等级编码 → 人可读中文标签映射.
 */
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

    private static final String DEFAULT_LABEL = "未知风险等级";

    public static String toLabel(String riskLevelCode) {
        if (riskLevelCode == null || riskLevelCode.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(riskLevelCode.trim().toUpperCase(java.util.Locale.ROOT), DEFAULT_LABEL);
    }
}
