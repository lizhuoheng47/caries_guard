package com.cariesguard.system.app;

import com.cariesguard.framework.security.authorization.PermissionAuthorityService;
import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemPermissionAuthorityService implements PermissionAuthorityService {

    private final SystemPermissionRepository systemPermissionRepository;

    public SystemPermissionAuthorityService(SystemPermissionRepository systemPermissionRepository) {
        this.systemPermissionRepository = systemPermissionRepository;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        return systemPermissionRepository.hasPermissionCode(userId, permissionCode.trim());
    }
}
