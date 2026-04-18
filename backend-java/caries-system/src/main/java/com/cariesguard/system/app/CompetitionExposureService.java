package com.cariesguard.system.app;

import com.cariesguard.system.config.CompetitionModeProperties;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompetitionExposureService {

    private final CompetitionModeProperties properties;

    public CompetitionExposureService(CompetitionModeProperties properties) {
        this.properties = properties;
    }

    public List<String> filterPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return permissions.stream()
                .filter(this::isPermissionExposed)
                .toList();
    }

    public boolean isPermissionExposed(String permissionCode) {
        if (!properties.isEnabled() || !StringUtils.hasText(permissionCode)) {
            return StringUtils.hasText(permissionCode);
        }
        String normalized = permissionCode.trim();
        if (properties.getHiddenPermissionCodes().contains(normalized)) {
            return false;
        }
        return properties.getHiddenPermissionPrefixes().stream()
                .noneMatch(normalized::startsWith);
    }

    public boolean isMenuExposed(String routePath, String permissionCode) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (StringUtils.hasText(routePath) && properties.getForceVisibleRoutePaths().contains(routePath.trim())) {
            return true;
        }
        if (StringUtils.hasText(permissionCode) && !isPermissionExposed(permissionCode)) {
            return false;
        }
        if (!StringUtils.hasText(routePath)) {
            return true;
        }
        return !properties.getHiddenRoutePaths().contains(routePath.trim());
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
