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
        List<ModelVersionRuntimeVO> modelVersions) {

    public ModelRuntimeVO(String currentModelVersion,
                          long recentTaskCount,
                          long successTaskCount,
                          long failedTaskCount,
                          BigDecimal successRate) {
        this(currentModelVersion, recentTaskCount, successTaskCount, failedTaskCount, successRate,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, List.of());
    }
}
