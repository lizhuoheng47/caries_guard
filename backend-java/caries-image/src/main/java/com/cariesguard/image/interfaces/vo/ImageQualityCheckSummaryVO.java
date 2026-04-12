package com.cariesguard.image.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ImageQualityCheckSummaryVO(
        String checkTypeCode,
        String checkResultCode,
        Integer qualityScore,
        List<String> issueCodes,
        LocalDateTime checkedAt) {
}
