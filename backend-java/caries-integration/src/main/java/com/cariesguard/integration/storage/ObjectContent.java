package com.cariesguard.integration.storage;

import org.springframework.core.io.Resource;

public record ObjectContent(
        Resource resource,
        String contentType,
        String originalFileName,
        long contentLength) {
}
