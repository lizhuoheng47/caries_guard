package com.cariesguard.framework.security.sensitive;

public record ProtectedValue(
        String encrypted,
        String hash,
        String masked) {
}
