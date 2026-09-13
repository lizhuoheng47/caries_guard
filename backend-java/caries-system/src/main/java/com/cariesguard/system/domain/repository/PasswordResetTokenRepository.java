package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.PasswordResetTokenModel;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    void invalidateActiveTokens(Long userId, LocalDateTime now);

    void create(PasswordResetTokenModel model);

    Optional<PasswordResetTokenModel> findLatestActive(Long userId, LocalDateTime now);

    void incrementAttempts(Long tokenId);

    void markUsed(Long tokenId, LocalDateTime usedAt);
}
