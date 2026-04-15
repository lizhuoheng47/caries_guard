package com.cariesguard.image.domain.model;

public record AttachmentObjectRegistrationModel(
        Long attachmentId,
        String bizModuleCode,
        Long bizId,
        String fileCategoryCode,
        String originalName,
        String bucketName,
        String objectKey,
        String contentType,
        Long fileSizeBytes,
        String md5,
        String visibilityCode,
        Long uploadUserId,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
