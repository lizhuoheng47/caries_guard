package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportAttachmentCreateModel(
        Long attachmentId,
        String bizModuleCode,
        Long bizId,
        String fileCategoryCode,
        String assetTypeCode,
        Long sourceAttachmentId,
        String fileName,
        String originalName,
        String bucketName,
        String objectKey,
        String contentType,
        String fileExt,
        Long fileSizeBytes,
        String md5,
        String storageProviderCode,
        String visibilityCode,
        String retentionPolicyCode,
        LocalDateTime expiredAt,
        Long uploadUserId,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
