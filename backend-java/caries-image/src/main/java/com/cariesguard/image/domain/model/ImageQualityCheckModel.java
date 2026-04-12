package com.cariesguard.image.domain.model;

import java.time.LocalDateTime;

public record ImageQualityCheckModel(
        Long imageId,
        String checkTypeCode,
        String checkResultCode,
        Integer qualityScore,
        Integer blurScore,
        Integer exposureScore,
        Integer integrityScore,
        Integer occlusionScore,
        String issueCodesJson,
        String suggestionText,
        LocalDateTime checkedAt) {
}
