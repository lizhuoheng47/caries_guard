package com.cariesguard.report.interfaces.vo;

import java.math.BigDecimal;

public record ReportRiskFactorVO(
        String code,
        BigDecimal weight,
        String source,
        String evidence) {
}
