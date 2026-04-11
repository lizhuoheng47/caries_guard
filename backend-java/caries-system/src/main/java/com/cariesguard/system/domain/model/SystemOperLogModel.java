package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;

public record SystemOperLogModel(
        String traceId,
        String moduleCode,
        String operationTypeCode,
        String operationName,
        String requestPath,
        String requestMethod,
        Long targetId,
        Long operatorUserId,
        Long orgId,
        boolean success,
        String resultCode,
        String errorMessage,
        LocalDateTime operationTime) {
}
