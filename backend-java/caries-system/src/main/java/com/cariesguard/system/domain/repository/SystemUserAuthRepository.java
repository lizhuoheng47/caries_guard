package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemUserAuthModel;
import java.util.Optional;

public interface SystemUserAuthRepository {

    Optional<SystemUserAuthModel> findByUsername(String username);

    Optional<SystemUserAuthModel> findByUserId(Long userId);
}
