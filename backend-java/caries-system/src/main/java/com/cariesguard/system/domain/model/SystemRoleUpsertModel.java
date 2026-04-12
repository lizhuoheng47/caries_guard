package com.cariesguard.system.domain.model;

import java.util.Set;

public record SystemRoleUpsertModel(
        Long roleId,
        String roleCode,
        String roleName,
        Integer roleSort,
        String dataScopeCode,
        String isBuiltin,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId,
        Set<Long> menuIds) {
}
