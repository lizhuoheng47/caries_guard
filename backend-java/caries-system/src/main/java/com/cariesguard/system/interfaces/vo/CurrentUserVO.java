package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record CurrentUserVO(
        Long userId,
        Long orgId,
        String username,
        String displayName,
        String userTypeCode,
        List<String> roleCodes) {
}
