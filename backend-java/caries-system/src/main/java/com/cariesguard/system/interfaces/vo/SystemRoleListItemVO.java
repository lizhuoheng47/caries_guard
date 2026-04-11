package com.cariesguard.system.interfaces.vo;

public record SystemRoleListItemVO(
        Long roleId,
        String roleCode,
        String roleName,
        int roleSort,
        String dataScopeCode,
        boolean builtin,
        Long orgId,
        String status) {
}
