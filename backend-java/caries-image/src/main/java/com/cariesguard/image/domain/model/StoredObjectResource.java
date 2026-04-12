package com.cariesguard.image.domain.model;

import org.springframework.core.io.Resource;

public record StoredObjectResource(
        Resource resource,
        String contentType,
        String originalFileName,
        long contentLength) {
}
