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

        long callbackTotalCount,
        long callbackSuccessCount,
        BigDecimal callbackSuccessRate,

        long visualAssetExpectedCount,
        long visualAssetGeneratedCount,
        BigDecimal visualAssetSuccessRate,

        long reviewSuggestedCount,
        long reviewCompletedCount,
        BigDecimal reviewCompletionRate,

        long riskAssessmentTriggeredCount,
        long riskAssessmentCoveredCount,
        BigDecimal riskOutputCoverage,

        long doctorReviewTotalCount,
        long doctorReviewAgreeCount,
        BigDecimal doctorReviewAgreementRate,

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
                "UNKNOWN", "UNKNOWN", "UNKNOWN",
                List.of());
    }
}
