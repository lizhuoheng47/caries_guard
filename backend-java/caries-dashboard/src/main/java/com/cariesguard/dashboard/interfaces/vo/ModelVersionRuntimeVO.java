package com.cariesguard.dashboard.interfaces.vo;

import java.math.BigDecimal;

public record ModelVersionRuntimeVO(
        String modelVersion,
        long taskCount,
        long successTaskCount,
        long failedTaskCount,
        BigDecimal successRate,
        BigDecimal averageInferenceMillis) {
}
