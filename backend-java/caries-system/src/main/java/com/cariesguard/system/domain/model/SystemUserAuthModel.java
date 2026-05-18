package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record SystemUserAuthModel(
        Long userId,
        Long orgId,
        Long deptId,
        String userNo,
        String username,
        String passwordHash,
        String nickName,
        String realNameMasked,
        String phoneMasked,
        String emailMasked,
        String avatarUrl,
        String userTypeCode,
        String genderCode,
        String certificateNoMasked,
        LocalDateTime lastLoginAt,
        String status,
        List<String> roleCodes) {
}
