package com.cariesguard.report.interfaces.vo;

public record ReportImageVO(
        Long imageId,
        Long attachmentId,
        String imageTypeCode,
        String qualityStatusCode,
        String primaryFlag,
        String bucketName,
        String objectKey,
        String originalName) {
}
