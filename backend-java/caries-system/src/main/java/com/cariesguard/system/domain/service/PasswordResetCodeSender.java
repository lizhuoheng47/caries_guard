package com.cariesguard.system.domain.service;

public interface PasswordResetCodeSender {

    void send(String recipient, String username, String verificationCode, int expiresInMinutes);
}
