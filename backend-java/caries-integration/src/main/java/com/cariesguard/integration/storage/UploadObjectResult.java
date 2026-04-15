package com.cariesguard.integration.storage;

public record UploadObjectResult(
        String bucketName,
        String objectKey,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String md5,
        String providerCode) {
}
