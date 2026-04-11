package com.cariesguard.system.interfaces.vo;

public record LoginTokenVO(
        String token,
        long expireIn,
        LoginUserVO user) {
}
