package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record SystemUserMutationVO(
        Long userId,
        String userNo,
        String username,
        String status,
        List<Long> roleIds) {
}
