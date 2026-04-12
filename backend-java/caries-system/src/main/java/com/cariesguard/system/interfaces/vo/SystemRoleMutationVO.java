package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record SystemRoleMutationVO(
        Long roleId,
        String roleCode,
        String status,
        List<Long> menuIds) {
}
