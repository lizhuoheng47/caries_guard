package com.cariesguard.analysis.interfaces.vo;

import java.util.Map;

/**
 * 修正原因分类编码 → 人可读中文标签映射.
 */
public final class CorrectionReasonCategoryLabels {

    private CorrectionReasonCategoryLabels() {
    }

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("OVER_GRADED", "AI 分级偏高"),
            Map.entry("UNDER_GRADED", "AI 分级偏低"),
            Map.entry("WRONG_TOOTH", "牙位识别错误"),
            Map.entry("MISSED_LESION", "遗漏龋齿病灶"),
            Map.entry("FALSE_POSITIVE", "假阳性"),
            Map.entry("FALSE_NEGATIVE", "假阴性"),
            Map.entry("SURFACE_ERROR", "牙面判断错误"),
            Map.entry("SEVERITY_MISMATCH", "严重程度不匹配"),
            Map.entry("ARTIFACT", "影像伪影干扰"),
            Map.entry("OTHER", "其他原因")
    );

    private static final String DEFAULT_LABEL = "修正原因";

    public static String toLabel(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(categoryCode.trim().toUpperCase(java.util.Locale.ROOT),
                DEFAULT_LABEL + "（" + categoryCode + "）");
    }
}
