package com.cariesguard.image.domain.model;

import java.time.LocalDateTime;

public record ImageViewModel(
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
        String bodyPositionCode) {
}
