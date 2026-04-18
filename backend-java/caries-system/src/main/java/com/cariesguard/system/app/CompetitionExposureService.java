package com.cariesguard.system.app;

import com.cariesguard.system.config.CompetitionModeProperties;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompetitionExposureService {

    private static final List<String> RETAINED_ENTRIES = List.of(
            "analysis",
            "review",
            "rag",
            "/dashboard/model-runtime");

    private static final List<String> ENFORCEMENT_SURFACES = List.of(
            "current-user permissions",
            "@RequirePermission authorization",
            "system menu queries",
            "role menu bindings");

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
        String normalized = normalize(permissionCode);
        if (!properties.isEnabled() || normalized == null) {
            return normalized != null;
        }
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
        String normalizedRoutePath = normalize(routePath);
        if (normalizedRoutePath != null && properties.getForceVisibleRoutePaths().contains(normalizedRoutePath)) {
            return true;
        }
        if (StringUtils.hasText(permissionCode) && !isPermissionExposed(permissionCode)) {
            return false;
        }
        if (normalizedRoutePath == null) {
            return true;
        }
        return !properties.getHiddenRoutePaths().contains(normalizedRoutePath);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public CompetitionModeSnapshot snapshot() {
        return new CompetitionModeSnapshot(
                properties.isEnabled(),
                properties.getHiddenPermissionPrefixes(),
                properties.getHiddenPermissionCodes(),
                properties.getHiddenRoutePaths(),
                properties.getForceVisibleRoutePaths(),
                RETAINED_ENTRIES,
                ENFORCEMENT_SURFACES);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
