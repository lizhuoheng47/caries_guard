package com.cariesguard.report.interfaces.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报告风险评估 VO — 比赛展示增强版.
 * <p>
 * 新增风险等级中文标签、跟踪建议文本，使比赛讲解时
 * 可以直观展示 AI 风险评估的完整证据链。
 */
public record ReportRiskAssessmentVO(
        Long riskAssessmentId,
        String overallRiskLevelCode,
        BigDecimal riskScore,
        String assessmentReportJson,
        Integer recommendedCycleDays,
        String followupSuggestion,
        Boolean reviewSuggested,
        String explanation,
        String fusionVersion,
        List<ReportRiskFactorVO> riskFactors,
        LocalDateTime assessedAt,

        /* ── 新增：比赛展示字段 ── */
        /** 风险等级中文标签 */
        String riskLevelLabel,
        /** 跟踪建议（结构化文本） */
        String trackingSuggestion,
        /** 风险解释（面向患者/评委的通俗解释） */
        String riskExplanation) {

    public ReportRiskAssessmentVO {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }

    /** 向后兼容的简化构造器 */
    public ReportRiskAssessmentVO(Long riskAssessmentId,
                                  String overallRiskLevelCode,
                                  String assessmentReportJson,
                                  Integer recommendedCycleDays,
                                  LocalDateTime assessedAt) {
        this(riskAssessmentId, overallRiskLevelCode, null, assessmentReportJson, recommendedCycleDays,
                null, null, null, null, List.of(), assessedAt,
                RiskLevelLabels.toLabel(overallRiskLevelCode), null, null);
    }
}
