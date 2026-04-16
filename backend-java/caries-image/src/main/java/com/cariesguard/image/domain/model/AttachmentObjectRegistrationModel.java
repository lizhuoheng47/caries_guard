package com.cariesguard.image.domain.model;

import java.time.LocalDateTime;

public record AttachmentObjectRegistrationModel(
        Long attachmentId,
        String bizModuleCode,
        Long bizId,
        String fileCategoryCode,
        String assetTypeCode,
        Long sourceAttachmentId,
        String originalName,
        String bucketName,
        String objectKey,
        String contentType,
        Long fileSizeBytes,
        String md5,
        String visibilityCode,
        String retentionPolicyCode,
        LocalDateTime expiredAt,
        Long uploadUserId,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId,
        String caseNo,
        String taskNo,
        String modelVersion,
        Long relatedImageId,
        String toothCode) {
}
