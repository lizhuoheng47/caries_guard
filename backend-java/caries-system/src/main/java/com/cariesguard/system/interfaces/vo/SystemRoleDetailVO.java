package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record SystemRoleDetailVO(
        Long roleId,
        String roleCode,
        String roleName,
        int roleSort,
        String dataScopeCode,
        boolean builtin,
        Long orgId,
        String status,
        String remark,
        List<Long> menuIds) {
}
