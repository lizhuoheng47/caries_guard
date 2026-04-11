package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;

public record SystemUserSummaryModel(
        Long userId,
        Long deptId,
        String userNo,
        String username,
        String nickName,
        String realNameMasked,
        String phoneMasked,
        String userTypeCode,
        Long orgId,
        String status,
        LocalDateTime lastLoginAt) {
}
