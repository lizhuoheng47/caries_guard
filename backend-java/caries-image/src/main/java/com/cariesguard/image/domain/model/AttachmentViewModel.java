package com.cariesguard.image.domain.model;

public record AttachmentViewModel(
        Long attachmentId,
        String fileName,
        String originalName,
        String bucketName,
        String objectKey,
        String contentType,
        String md5,
        Long fileSizeBytes,
        Long orgId) {
}
