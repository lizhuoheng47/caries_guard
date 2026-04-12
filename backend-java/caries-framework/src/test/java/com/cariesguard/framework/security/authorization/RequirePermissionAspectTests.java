package com.cariesguard.framework.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequirePermissionAspectTests {

    private final PermissionAuthorityService permissionAuthorityService = mock(PermissionAuthorityService.class);
    private final RequirePermissionAspect requirePermissionAspect = new RequirePermissionAspect(permissionAuthorityService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowAdminBypass() throws Throwable {
        setCurrentUser("SYS_ADMIN");
        ProceedingJoinPoint joinPoint = mockJoinPoint("secured");

        Object result = requirePermissionAspect.around(joinPoint, permission("secured"));

        assertThat(result).isEqualTo("ok");
        verifyNoInteractions(permissionAuthorityService);
    }

    @Test
    void shouldAllowWhenPermissionGranted() throws Throwable {
        setCurrentUser("DOCTOR");
        when(permissionAuthorityService.hasPermission(100001L, "system:user:list")).thenReturn(true);

        Object result = requirePermissionAspect.around(mockJoinPoint("secured"), permission("secured"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void shouldRejectWhenPermissionMissing() {
        setCurrentUser("DOCTOR");
        when(permissionAuthorityService.hasPermission(100001L, "system:user:list")).thenReturn(false);

        assertThatThrownBy(() -> requirePermissionAspect.around(mockJoinPoint("secured"), permission("secured")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(CommonErrorCode.FORBIDDEN.code());
    }

    private void setCurrentUser(String roleCode) {
        AuthenticatedUser user = new AuthenticatedUser(
                100001L,
                100001L,
                "tester",
                "encoded",
                "Tester",
                true,
                List.of(roleCode));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName) throws Throwable {
        Method method = PermissionTarget.class.getDeclaredMethod(methodName);
        MethodSignature methodSignature = mock(MethodSignature.class);
        when(methodSignature.getMethod()).thenReturn(method);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.proceed()).thenReturn("ok");
        return joinPoint;
    }

    private RequirePermission permission(String methodName) throws Exception {
        Method method = PermissionTarget.class.getDeclaredMethod(methodName);
        return method.getAnnotation(RequirePermission.class);
    }

    static class PermissionTarget {

        @RequirePermission("system:user:list")
        public String secured() {
            return "ok";
        }
    }
}
