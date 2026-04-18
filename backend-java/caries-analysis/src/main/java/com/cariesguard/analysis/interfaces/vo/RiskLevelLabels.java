package com.cariesguard.analysis.interfaces.vo;

import java.util.Map;

/**
 * 风险等级编码 → 人可读中文标签映射.
 * <p>
 * 比赛展示时，评委可以直接看到中文风险等级而非内部编码。
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

    /**
     * 将风险等级编码转为中文标签.
     *
     * @param riskLevel 编码，如 "HIGH"
     * @return 中文标签；null 输入返回 null
     */
    public static String toLabel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(riskLevel.trim().toUpperCase(java.util.Locale.ROOT), riskLevel);
    }
}
