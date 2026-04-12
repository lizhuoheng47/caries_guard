package com.cariesguard.image.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ImageQualityCheckVO(
        Long imageId,
        String checkTypeCode,
        String checkResultCode,
        Integer qualityScore,
        Integer blurScore,
        Integer exposureScore,
        Integer integrityScore,
        Integer occlusionScore,
        List<String> issueCodes,
        String suggestionText,
        LocalDateTime checkedAt) {
}
