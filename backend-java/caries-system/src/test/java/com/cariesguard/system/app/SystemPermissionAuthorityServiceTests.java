package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemPermissionAuthorityServiceTests {

    @Mock
    private SystemPermissionRepository systemPermissionRepository;

    @Test
    void shouldReturnTrueWhenPermissionExists() {
        SystemPermissionAuthorityService service = new SystemPermissionAuthorityService(systemPermissionRepository);
        when(systemPermissionRepository.hasPermissionCode(100001L, "system:user:list")).thenReturn(true);

        boolean result = service.hasPermission(100001L, "system:user:list");

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPermissionCodeBlank() {
        SystemPermissionAuthorityService service = new SystemPermissionAuthorityService(systemPermissionRepository);

        boolean result = service.hasPermission(100001L, " ");

        assertThat(result).isFalse();
    }
}
