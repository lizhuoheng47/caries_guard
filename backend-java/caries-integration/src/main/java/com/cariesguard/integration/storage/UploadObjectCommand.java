package com.cariesguard.integration.storage;

import java.io.InputStream;

public record UploadObjectCommand(
        String bucketName,
        String objectKey,
        String fileName,
        String contentType,
        InputStream inputStream,
        long fileSizeBytes,
        String md5) {
}
