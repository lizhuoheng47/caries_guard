package com.cariesguard.image.interfaces.vo;

import java.time.LocalDateTime;

public record ImageVO(
        Long imageId,
        Long attachmentId,
        String originalName,
        String bucketName,
        String objectKey,
        String imageTypeCode,
        String imageSourceCode,
        String qualityStatusCode,
        String primaryFlag,
        LocalDateTime shootingTime,
        String bodyPositionCode,
        ImageQualityCheckSummaryVO currentQualityCheck) {
}
