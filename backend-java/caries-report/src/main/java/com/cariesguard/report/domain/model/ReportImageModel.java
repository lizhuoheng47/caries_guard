package com.cariesguard.report.domain.model;

public record ReportImageModel(
        Long imageId,
        Long attachmentId,
        String imageTypeCode,
        String qualityStatusCode,
        String primaryFlag,
        String bucketName,
        String objectKey,
        String originalName) {
}

