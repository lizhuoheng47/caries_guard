package com.cariesguard.system.app;

import java.util.List;

public record CompetitionModeSnapshot(
        boolean enabled,
        List<String> hiddenPermissionPrefixes,
        List<String> hiddenPermissionCodes,
        List<String> hiddenRoutePaths,
        List<String> forceVisibleRoutePaths,
        List<String> retainedEntries,
        List<String> enforcementSurfaces) {
}
