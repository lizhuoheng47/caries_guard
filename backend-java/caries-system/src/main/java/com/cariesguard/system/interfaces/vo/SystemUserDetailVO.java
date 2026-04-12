package com.cariesguard.system.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SystemUserDetailVO(
        Long userId,
        Long deptId,
        String userNo,
        String username,
        String nickName,
        String realNameMasked,
        String phoneMasked,
        String emailMasked,
        String avatarUrl,
        String userTypeCode,
        String genderCode,
        String certificateTypeCode,
        String certificateNoMasked,
        Long orgId,
        String status,
        String remark,
        LocalDateTime lastLoginAt,
        LocalDateTime pwdUpdatedAt,
        List<Long> roleIds,
        List<String> roleCodes) {
}
