package com.cariesguard.image.interfaces.vo;

public record AttachmentUploadVO(
        Long attachmentId,
        String fileName,
        String bucketName,
        String objectKey,
        String md5) {
}
