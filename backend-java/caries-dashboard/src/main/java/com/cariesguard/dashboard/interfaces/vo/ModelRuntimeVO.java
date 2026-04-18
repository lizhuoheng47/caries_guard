package com.cariesguard.dashboard.interfaces.vo;

import java.math.BigDecimal;
import java.util.List;

public record ModelRuntimeVO(
        String currentModelVersion,
        long recentTaskCount,
        long successTaskCount,
        long failedTaskCount,
        BigDecimal successRate,
        BigDecimal averageInferenceMillis,
        BigDecimal highUncertaintyRate,
        BigDecimal reviewSuggestedRate,
        long correctionFeedbackCount,
        
        // AI Governance Callback Metrics
        long callbackTotalCount,
        long callbackSuccessCount,
        BigDecimal callbackSuccessRate,
        
        // AI Governance Visual Asset Metrics
        long visualAssetExpectedCount,
        long visualAssetGeneratedCount,
        BigDecimal visualAssetSuccessRate,
        
        // AI Governance Review Metrics
        long reviewSuggestedCount,
        long reviewCompletedCount,
        BigDecimal reviewCompletionRate,
        
        // AI Governance Risk Output Metrics
        long riskAssessmentTriggeredCount,
        long riskAssessmentCoveredCount,
        BigDecimal riskOutputCoverage,
        
        // AI Governance Evidence/Citation Metrics
        long ragRequestCount,
        long citationPresentCount,
        BigDecimal citationCompleteness,
        
        // AI Governance Doctor Agreement Metrics
        long doctorReviewTotalCount,
        long doctorReviewAgreeCount,
        BigDecimal doctorReviewAgreementRate,
        
        // System & Model Information
        String knowledgeVersion,
        String runtimeMode,
        String llmProviderCode,
        String llmModelName,

        List<ModelVersionRuntimeVO> modelVersions) {

    public ModelRuntimeVO(String currentModelVersion,
                          long recentTaskCount,
                          long successTaskCount,
                          long failedTaskCount,
                          BigDecimal successRate) {
        this(currentModelVersion, recentTaskCount, successTaskCount, failedTaskCount, successRate,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                0L, 0L, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO,
                "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN",
                List.of());
    }
}
