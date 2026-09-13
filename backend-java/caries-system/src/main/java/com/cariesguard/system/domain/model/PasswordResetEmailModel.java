package com.cariesguard.system.domain.model;

public record PasswordResetEmailModel(String encryptedEmail, String maskedEmail) {
}
