package com.cariesguard.image.interfaces.command;

import java.util.List;

public record SaveImageQualityCheckCommand(
        String checkTypeCode,
        String checkResultCode,
        Integer qualityScore,
        Integer blurScore,
        Integer exposureScore,
        Integer integrityScore,
        Integer occlusionScore,
        List<String> issueCodes,
        String suggestionText,
        String remark) {
}
