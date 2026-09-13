package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;

public record PasswordResetTokenModel(
        Long id,
        Long userId,
        String username,
        String codeSalt,
        String codeHash,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        int attemptCount,
        String requestIp,
        LocalDateTime createdAt) {
}
