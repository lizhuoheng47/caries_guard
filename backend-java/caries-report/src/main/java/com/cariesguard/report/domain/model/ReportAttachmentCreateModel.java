package com.cariesguard.report.domain.model;

public record ReportAttachmentCreateModel(
        Long attachmentId,
        String bizModuleCode,
        Long bizId,
        String fileCategoryCode,
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
        Long uploadUserId,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}

