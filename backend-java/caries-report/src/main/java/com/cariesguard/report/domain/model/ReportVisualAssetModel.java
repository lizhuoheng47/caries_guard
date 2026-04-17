package com.cariesguard.report.domain.model;

public record ReportVisualAssetModel(
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
