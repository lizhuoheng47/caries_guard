package com.cariesguard.report.interfaces.vo;

import java.util.Map;

/**
 * 风险因子编码 → 人可读中文标签映射.
 */
public final class RiskFactorLabels {

    private RiskFactorLabels() {
    }

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("SEVERITY", "龋齿严重程度"),
            Map.entry("MULTI_TOOTH", "多牙受累"),
            Map.entry("UNCERTAINTY", "模型不确定性"),
            Map.entry("HISTORY", "既往龋齿史"),
            Map.entry("AGE", "年龄因素"),
            Map.entry("HYGIENE", "口腔卫生状况"),
            Map.entry("DIET", "饮食习惯"),
            Map.entry("FLUORIDE", "氟化物使用"),
            Map.entry("SALIVA", "唾液因素"),
            Map.entry("PROGRESSION", "病灶进展速度"),
            Map.entry("LOCATION", "病灶位置风险")
    );

    private static final String DEFAULT_LABEL = "风险因子";

    public static String toLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(code.trim().toUpperCase(java.util.Locale.ROOT),
                DEFAULT_LABEL + "（" + code + "）");
    }
}
