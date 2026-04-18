package com.cariesguard.dashboard.interfaces.vo;

/**
 * 风险等级分布 VO — 比赛展示增强版.
 * <p>
 * 增加中文标签和总计，使 Dashboard 展示更直观。
 */
public record RiskLevelDistributionVO(
        long highRiskCount,
        long mediumRiskCount,
        long lowRiskCount,
        /** 总计 */
        long totalCount,
        /** 高风险占比 */
        String highRiskRatio,
        /** 中风险占比 */
        String mediumRiskRatio,
        /** 低风险占比 */
        String lowRiskRatio) {

    /** 向后兼容的简化构造器 — 自动计算占比 */
    public RiskLevelDistributionVO(long highRiskCount,
                                   long mediumRiskCount,
                                   long lowRiskCount) {
        this(highRiskCount, mediumRiskCount, lowRiskCount,
                highRiskCount + mediumRiskCount + lowRiskCount,
                ratio(highRiskCount, highRiskCount + mediumRiskCount + lowRiskCount),
                ratio(mediumRiskCount, highRiskCount + mediumRiskCount + lowRiskCount),
                ratio(lowRiskCount, highRiskCount + mediumRiskCount + lowRiskCount));
    }

    private static String ratio(long count, long total) {
        if (total == 0) {
            return "0.0%";
        }
        return String.format("%.1f%%", count * 100.0 / total);
    }
}
