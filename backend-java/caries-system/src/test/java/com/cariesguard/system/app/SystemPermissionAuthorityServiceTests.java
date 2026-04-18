package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.system.config.CompetitionModeProperties;
import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemPermissionAuthorityServiceTests {

    @Mock
    private SystemPermissionRepository systemPermissionRepository;

    private CompetitionExposureService competitionExposureService(boolean enabled) {
        CompetitionModeProperties properties = new CompetitionModeProperties();
        properties.setEnabled(enabled);
        return new CompetitionExposureService(properties);
    }

    @Test
    void shouldReturnTrueWhenPermissionExists() {
        SystemPermissionAuthorityService service =
                new SystemPermissionAuthorityService(systemPermissionRepository, competitionExposureService(false));
        when(systemPermissionRepository.hasPermissionCode(100001L, "system:user:list")).thenReturn(true);

        boolean result = service.hasPermission(100001L, "system:user:list");

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPermissionCodeBlank() {
        SystemPermissionAuthorityService service =
                new SystemPermissionAuthorityService(systemPermissionRepository, competitionExposureService(false));

        boolean result = service.hasPermission(100001L, " ");

        assertThat(result).isFalse();
    }

    @Test
    void competitionModeShouldDenyHiddenPermissions() {
        SystemPermissionAuthorityService service =
                new SystemPermissionAuthorityService(systemPermissionRepository, competitionExposureService(true));

        boolean result = service.hasPermission(100001L, "followup:task:view");

        assertThat(result).isFalse();
    }

    @Test
    void competitionModeShouldDenyGeneralDashboardButKeepOpsDashboard() {
        SystemPermissionAuthorityService service =
                new SystemPermissionAuthorityService(systemPermissionRepository, competitionExposureService(true));
        when(systemPermissionRepository.hasPermissionCode(100001L, "dashboard:ops:view")).thenReturn(true);

        boolean hiddenResult = service.hasPermission(100001L, "dashboard:view");
        boolean exposedResult = service.hasPermission(100001L, "dashboard:ops:view");

        assertThat(hiddenResult).isFalse();
        assertThat(exposedResult).isTrue();
    }
}
