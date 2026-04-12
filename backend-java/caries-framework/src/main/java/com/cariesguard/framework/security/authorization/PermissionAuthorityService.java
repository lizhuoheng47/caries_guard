package com.cariesguard.framework.security.authorization;

public interface PermissionAuthorityService {

    boolean hasPermission(Long userId, String permissionCode);
}
