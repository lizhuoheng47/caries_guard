package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemConfigModel;
import java.util.Optional;

public interface SystemConfigRepository {

    Optional<SystemConfigModel> findActiveByKey(String configKey);
}
