package com.cariesguard.system.interfaces.vo;

public record LoginTokenVO(
        String tokenType,
        String accessToken,
        long expiresIn) {
}
