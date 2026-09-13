package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.model.PasswordResetEmailModel;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SystemUserAuthRepository {

    Optional<SystemUserAuthModel> findByUsername(String username);

    Optional<SystemUserAuthModel> findByUserId(Long userId);

    Optional<PasswordResetEmailModel> findPasswordResetEmail(Long userId);

    void markLoginSuccess(Long userId, LocalDateTime loginTime);

    void updatePassword(Long userId, String passwordHash, LocalDateTime updatedAt);
}
