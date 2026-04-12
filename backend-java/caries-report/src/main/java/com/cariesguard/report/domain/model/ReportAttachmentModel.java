package com.cariesguard.report.domain.model;

public record ReportAttachmentModel(
        Long attachmentId,
        String bucketName,
        String objectKey,
        String originalName,
        String contentType,
        Long orgId,
        String status) {
}

