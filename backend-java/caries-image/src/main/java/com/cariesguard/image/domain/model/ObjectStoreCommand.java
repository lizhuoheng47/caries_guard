package com.cariesguard.image.domain.model;

import java.io.InputStream;

public record ObjectStoreCommand(
        String bucketCode,
        String keyModule,
        String bizId,
        String originalFileName,
        String contentType,
        InputStream inputStream,
        long fileSizeBytes,
        String md5) {
}
