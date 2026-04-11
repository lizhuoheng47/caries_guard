package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.jwt.JwtTokenProvider;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import com.cariesguard.system.interfaces.command.LoginCommand;
import com.cariesguard.system.interfaces.vo.CurrentUserVO;
import com.cariesguard.system.interfaces.vo.LoginTokenVO;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceTests {

    @Mock
    private SystemUserAuthRepository systemUserAuthRepository;

    @Mock
    private SystemPermissionRepository systemPermissionRepository;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldReturnAlignedPayloadAndWriteAudit() {
        AuthAppService authAppService = new AuthAppService(
                systemUserAuthRepository,
                systemPermissionRepository,
                loginAuditService,
                passwordEncoder,
                jwtTokenProvider);
        SystemUserAuthModel user = new SystemUserAuthModel(
                100001L,
                100001L,
                "admin",
                "hash",
                "Admin",
                "ADMIN",
                "ACTIVE",
                List.of("SYS_ADMIN"));
        LoginCommand command = new LoginCommand();
        command.setUsername("admin");
        command.setPassword("123456");
        when(systemUserAuthRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(AuthenticatedUser.class))).thenReturn("jwt-token");
        when(jwtTokenProvider.getAccessTokenExpireSeconds()).thenReturn(7200L);

        LoginTokenVO result = authAppService.login(command, httpServletRequest);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.expireIn()).isEqualTo(7200L);
        assertThat(result.user().nickName()).isEqualTo("Admin");
        verify(systemUserAuthRepository).markLoginSuccess(eq(100001L), any());
        verify(loginAuditService).recordSuccess(user, httpServletRequest);
    }

    @Test
    void currentUserShouldIncludePermissions() {
        AuthAppService authAppService = new AuthAppService(
                systemUserAuthRepository,
                systemPermissionRepository,
                loginAuditService,
                passwordEncoder,
                jwtTokenProvider);
        SystemUserAuthModel user = new SystemUserAuthModel(
                100001L,
                100001L,
                "admin",
                "hash",
                "Admin",
                "ADMIN",
                "ACTIVE",
                List.of("SYS_ADMIN"));
        AuthenticatedUser principal = authAppService.toAuthenticatedUser(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        when(systemUserAuthRepository.findByUserId(100001L)).thenReturn(Optional.of(user));
        when(systemPermissionRepository.findPermissionCodesByUserId(100001L))
                .thenReturn(List.of("system:user:list", "patient:create"));

        CurrentUserVO result = authAppService.currentUser();

        assertThat(result.roles()).containsExactly("SYS_ADMIN");
        assertThat(result.permissions()).containsExactly("system:user:list", "patient:create");
        assertThat(result.nickName()).isEqualTo("Admin");
    }
}
