package com.cariesguard.system.domain.model;

import java.util.List;

public record SystemUserAuthModel(
        Long userId,
        Long orgId,
        String username,
        String passwordHash,
        String nickName,
        String userTypeCode,
        String status,
        List<String> roleCodes) {
}
