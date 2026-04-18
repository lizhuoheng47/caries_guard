package com.cariesguard.report.interfaces.vo;

import java.math.BigDecimal;

/**
 * 风险因子 VO — 比赛展示增强版.
 */
public record ReportRiskFactorVO(
        /** 因子编码 */
        String code,
        /** 权重 */
        BigDecimal weight,
        /** 来源 */
        String source,
        /** 证据描述 */
        String evidence,
        /** 因子中文标签 */
        String label) {

    /** 向后兼容的简化构造器 */
    public ReportRiskFactorVO(String code,
                              BigDecimal weight,
                              String source,
                              String evidence) {
        this(code, weight, source, evidence, RiskFactorLabels.toLabel(code));
    }
}
