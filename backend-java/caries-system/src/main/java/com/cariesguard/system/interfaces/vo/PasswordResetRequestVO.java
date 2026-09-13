package com.cariesguard.system.interfaces.vo;

public record PasswordResetRequestVO(
        String message,
        long expiresInSeconds,
        String deliveryMasked,
        String developmentCode) {
}
