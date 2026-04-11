package com.cariesguard.framework.security.context;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(
                    CommonErrorCode.AUTHENTICATION_REQUIRED.code(),
                    CommonErrorCode.AUTHENTICATION_REQUIRED.message());
        }
        return user;
    }

    public static DataScopeContext currentDataScope() {
        AuthenticatedUser currentUser = currentUser();
        DataScopeType scopeType = currentUser.hasAnyRole("ADMIN", "SYS_ADMIN")
                ? DataScopeType.ALL
                : DataScopeType.ORG;
        return new DataScopeContext(
                currentUser.getUserId(),
                currentUser.getOrgId(),
                currentUser.getRoleCodes(),
                scopeType,
                Set.of());
    }
}
