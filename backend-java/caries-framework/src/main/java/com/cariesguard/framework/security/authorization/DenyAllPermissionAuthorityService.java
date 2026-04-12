package com.cariesguard.framework.security.authorization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PermissionAuthorityService.class)
public class DenyAllPermissionAuthorityService implements PermissionAuthorityService {

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        return false;
    }
}
