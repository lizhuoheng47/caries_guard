package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * AI Evidence Display VO — 比赛展示用分析摘要.
 * <p>
 * 在原有后台字段基础上，增加了 AI 证据链所需的结构化字段，
 * 使评委/观众可以直观看到 AI 推理过程与置信度。
 */
public record AnalysisSummaryVO(
        /* ── 原有字段 ── */
        String overallHighestSeverity,
        Double uncertaintyScore,
        String reviewSuggestedFlag,
        Integer lesionCount,
        Integer abnormalToothCount,
        Integer summaryVersionNo,
        Integer teethCount,
        String riskLevel,
        String reviewReason,
        String doctorReviewRequiredReason,
        String knowledgeVersion,
        JsonNode riskFactors,
        JsonNode evidenceRefs,

        /* ── 新增：AI 证据展示字段 ── */
        /** 分级标签，如 ENAMEL / DENTIN / PULP */
        String gradingLabel,
        /** 模型置信度 0-1 */
        Double confidenceScore,
        /** 是否需要人工复核 */
        Boolean needsReview,
        /** 跟踪/随访建议 */
        String followUpRecommendation,
        /** reviewReason 的人可读中文标签 */
        String reviewReasonLabel,
        /** 按类型分组的证据引用 */
        Map<String, List<EvidenceRefItemVO>> classifiedEvidenceRefs,
        /** 知识库引用 citations */
        List<AnalysisCitationVO> citations,
        /** 折叠展示的原始 JSON（前端默认折叠） */
        JsonNode rawResultJson,
        /** 前端展示提示：rawResultJson 默认折叠 */
        Boolean rawResultJsonCollapsed,
        /** 风险等级中文标签 */
        String riskLevelLabel) {

    /** 向后兼容的简化构造器 */
    public AnalysisSummaryVO(String overallHighestSeverity,
                             Double uncertaintyScore,
                             String reviewSuggestedFlag,
                             Integer teethCount) {
        this(overallHighestSeverity, uncertaintyScore, reviewSuggestedFlag,
                null, null, null, teethCount,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, true, null);
    }
}
