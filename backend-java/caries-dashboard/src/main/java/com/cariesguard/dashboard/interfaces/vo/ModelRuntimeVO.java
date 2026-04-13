package com.cariesguard.dashboard.interfaces.vo;

import java.math.BigDecimal;

public record ModelRuntimeVO(
        String currentModelVersion,
        long recentTaskCount,
        long successTaskCount,
        long failedTaskCount,
        BigDecimal successRate) {
}
