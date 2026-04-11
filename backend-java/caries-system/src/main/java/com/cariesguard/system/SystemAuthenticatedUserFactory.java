package com.cariesguard.system;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemUserAuthModel;

public final class SystemAuthenticatedUserFactory {

    private SystemAuthenticatedUserFactory() {
    }

    public static AuthenticatedUser fromModel(SystemUserAuthModel user) {
        return new AuthenticatedUser(
                user.userId(),
                user.orgId(),
                user.username(),
                user.passwordHash(),
                user.nickName(),
                "ACTIVE".equals(user.status()),
                user.roleCodes());
    }
}
