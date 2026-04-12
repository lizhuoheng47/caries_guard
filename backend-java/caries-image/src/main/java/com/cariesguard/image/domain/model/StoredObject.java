package com.cariesguard.image.domain.model;

public record StoredObject(
        String bucketName,
        String objectKey,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String md5,
        String providerCode) {
}
