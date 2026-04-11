package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.DefaultMaskingService;
import com.cariesguard.system.domain.model.SystemConfigModel;
import com.cariesguard.system.domain.repository.SystemConfigRepository;
import com.cariesguard.system.domain.repository.SystemDictionaryRepository;
import com.cariesguard.system.interfaces.vo.SystemConfigVO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SystemQueryAppServiceTests {

    @Mock
    private SystemDictionaryRepository systemDictionaryRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private SystemDataScopeService systemDataScopeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getConfigShouldMaskSensitiveValueForNonAdmin() {
        SystemQueryAppService appService = new SystemQueryAppService(
                systemDictionaryRepository,
                systemConfigRepository,
                systemDataScopeService,
                new DefaultMaskingService());
        AuthenticatedUser principal = new AuthenticatedUser(
                200001L,
                100001L,
                "doctor",
                "n/a",
                "Doctor",
                true,
                List.of("DOCTOR"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        when(systemConfigRepository.findActiveByKey("oss.secret"))
                .thenReturn(Optional.of(new SystemConfigModel(
                        "oss.secret",
                        "Object Storage Secret",
                        "very-secret-value",
                        "STRING",
                        true)));

        SystemConfigVO result = appService.getConfig("oss.secret");

        assertThat(result.configValue()).isNotEqualTo("very-secret-value");
        assertThat(result.sensitive()).isTrue();
    }
}
