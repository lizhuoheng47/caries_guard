package com.cariesguard.system.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

public record SystemUserUpsertModel(
        Long userId,
        Long deptId,
        String userNo,
        String username,
        String passwordHash,
        String realNameEnc,
        String realNameHash,
        String realNameMasked,
        String nickName,
        String userTypeCode,
        String genderCode,
        String phoneEnc,
        String phoneHash,
        String phoneMasked,
        String emailEnc,
        String emailHash,
        String emailMasked,
        String avatarUrl,
        String certificateTypeCode,
        String certificateNoEnc,
        String certificateNoHash,
        String certificateNoMasked,
        LocalDateTime pwdUpdatedAt,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId,
        Set<Long> roleIds) {
}
