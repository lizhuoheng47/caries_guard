package com.cariesguard.report.interfaces.vo;

public record ReportVisualAssetVO(
        Long visualAssetId,
        Long taskId,
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder,
        String bucketName,
        String objectKey,
        String contentType,
        String originalName) {
}
