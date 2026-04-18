package com.cariesguard.system.app;

import com.cariesguard.framework.security.authorization.PermissionAuthorityService;
import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemPermissionAuthorityService implements PermissionAuthorityService {

    private final SystemPermissionRepository systemPermissionRepository;
    private final CompetitionExposureService competitionExposureService;

    public SystemPermissionAuthorityService(SystemPermissionRepository systemPermissionRepository,
                                           CompetitionExposureService competitionExposureService) {
        this.systemPermissionRepository = systemPermissionRepository;
        this.competitionExposureService = competitionExposureService;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        String normalized = permissionCode.trim();
        if (!competitionExposureService.isPermissionExposed(normalized)) {
            return false;
        }
        return systemPermissionRepository.hasPermissionCode(userId, normalized);
    }
}
