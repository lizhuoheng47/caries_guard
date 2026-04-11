package com.cariesguard.system.interfaces.vo;

import java.time.LocalDateTime;

public record SystemUserListItemVO(
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
