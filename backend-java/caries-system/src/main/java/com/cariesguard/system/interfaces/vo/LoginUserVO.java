package com.cariesguard.system.interfaces.vo;

public record LoginUserVO(
        Long userId,
        String username,
        String nickName,
        String userTypeCode,
        Long orgId) {
}
