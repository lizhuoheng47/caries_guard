package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;

public record SystemLoginLogModel(
        String traceId,
        String username,
        Long userId,
        Long orgId,
        String loginStatusCode,
        String clientIp,
        String userAgent,
        String failureReason,
        LocalDateTime loginTime) {
}
