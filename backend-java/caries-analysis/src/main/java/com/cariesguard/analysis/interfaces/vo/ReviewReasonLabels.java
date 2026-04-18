package com.cariesguard.analysis.interfaces.vo;

import java.util.Map;

/**
 * reviewReason 编码 → 人可读中文标签映射.
 * <p>
 * 比赛展示时，评委可以直接看到中文原因而非内部编码。
 */
public final class ReviewReasonLabels {

    private ReviewReasonLabels() {
    }

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("HIGH_UNCERTAINTY", "模型不确定性较高"),
            Map.entry("LOW_CONFIDENCE", "模型置信度偏低"),
            Map.entry("BOUNDARY_CASE", "分级边界病例"),
            Map.entry("MULTI_LESION", "多发龋齿需综合判断"),
            Map.entry("RARE_PATTERN", "罕见影像模式"),
            Map.entry("MODEL_DISAGREEMENT", "多模型结果不一致"),
            Map.entry("QUALITY_ISSUE", "影像质量不足"),
            Map.entry("CLINICAL_RISK", "临床风险因素触发"),
            Map.entry("KNOWLEDGE_GAP", "知识库覆盖不足"),
            Map.entry("PATIENT_HISTORY", "患者历史需人工核实"),
            Map.entry("SEVERITY_ESCALATION", "严重程度升级需确认"),
            Map.entry("FIRST_VISIT", "初诊患者需基线确认"),
            Map.entry("PEDIATRIC", "儿童患者需专科判断"),
            Map.entry("MANUAL_REQUEST", "医生主动请求复核")
    );

    private static final String DEFAULT_LABEL = "需要人工复核";

    /**
     * 将 reviewReason 编码转为中文标签.
     *
     * @param reasonCode 编码，如 "HIGH_UNCERTAINTY"
     * @return 中文标签；未知编码返回默认标签
     */
    public static String toLabel(String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return null;
        }
        return LABELS.getOrDefault(reasonCode.trim().toUpperCase(java.util.Locale.ROOT), DEFAULT_LABEL + "（" + reasonCode + "）");
    }
}
