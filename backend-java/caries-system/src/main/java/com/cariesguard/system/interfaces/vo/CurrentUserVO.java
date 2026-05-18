package com.cariesguard.system.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CurrentUserVO(
        Long userId,
        String username,
        String nickName,
        String realNameMasked,
        Long deptId,
        String userNo,
        String phoneMasked,
        String emailMasked,
        String avatarUrl,
        String userTypeCode,
        String genderCode,
        String certificateNoMasked,
        Long orgId,
        LocalDateTime lastLoginAt,
        String status,
        List<String> roles,
        List<String> permissions,
        boolean competitionModeEnabled) {
}
