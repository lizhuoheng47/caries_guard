package com.cariesguard.image.domain.model;

public record AttachmentDuplicateModel(
        Long attachmentId,
        String fileName,
        String originalName,
        String bucketName,
        String objectKey,
        String md5,
        String contentType,
        Long fileSizeBytes) {
}
