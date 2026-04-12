package com.cariesguard.system.domain.repository;

import java.util.List;

public interface SystemPermissionRepository {

    List<String> findPermissionCodesByUserId(Long userId);

    boolean hasPermissionCode(Long userId, String permissionCode);
}
