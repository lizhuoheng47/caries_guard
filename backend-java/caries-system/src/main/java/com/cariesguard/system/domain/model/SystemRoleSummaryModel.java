package com.cariesguard.system.domain.model;

public record SystemRoleSummaryModel(
        Long roleId,
        String roleCode,
        String roleName,
        int roleSort,
        String dataScopeCode,
        boolean builtin,
        Long orgId,
        String status) {
}
