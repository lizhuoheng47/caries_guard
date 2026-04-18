package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record CurrentUserVO(
        Long userId,
        String username,
        String nickName,
        String userTypeCode,
        Long orgId,
        List<String> roles,
        List<String> permissions,
        boolean competitionModeEnabled) {
}
