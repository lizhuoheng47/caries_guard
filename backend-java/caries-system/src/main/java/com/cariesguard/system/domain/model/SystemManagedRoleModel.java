package com.cariesguard.system.domain.model;

import java.util.List;

public record SystemManagedRoleModel(
        Long roleId,
        Long orgId,
        String roleCode,
        String isBuiltin,
        List<Long> menuIds) {
}
