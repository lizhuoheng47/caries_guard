package com.cariesguard.system.domain.model;

import java.util.List;

public record SystemRoleDetailModel(
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
