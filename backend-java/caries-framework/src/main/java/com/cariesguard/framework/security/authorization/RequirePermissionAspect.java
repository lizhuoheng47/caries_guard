package com.cariesguard.framework.security.authorization;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermissionAspect {

    private final PermissionAuthorityService permissionAuthorityService;

    public RequirePermissionAspect(PermissionAuthorityService permissionAuthorityService) {
        this.permissionAuthorityService = permissionAuthorityService;
    }

    @Around("@within(requirePermission) || @annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        AuthenticatedUser currentUser = SecurityContextUtils.currentUser();
        if (!currentUser.hasAnyRole("ADMIN", "SYS_ADMIN")
                && !permissionAuthorityService.hasPermission(currentUser.getUserId(), requirePermission.value())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }
}
