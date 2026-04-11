package com.cariesguard.system.domain.model;

import java.util.List;

public record SystemManagedUserModel(
        Long userId,
        Long orgId,
        String userNo,
        String username,
        String status,
        List<Long> roleIds) {
}
