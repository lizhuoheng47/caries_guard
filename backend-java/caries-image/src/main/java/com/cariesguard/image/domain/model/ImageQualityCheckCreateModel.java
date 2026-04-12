package com.cariesguard.image.domain.model;

import java.time.LocalDateTime;

public record ImageQualityCheckCreateModel(
        Long checkId,
        Long imageId,
        Long caseId,
        Long patientId,
        String checkTypeCode,
        String checkResultCode,
        Integer qualityScore,
        Integer blurScore,
        Integer exposureScore,
        Integer integrityScore,
        Integer occlusionScore,
        String issueCodesJson,
        String suggestionText,
        String currentFlag,
        Long checkedBy,
        LocalDateTime checkedAt,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
