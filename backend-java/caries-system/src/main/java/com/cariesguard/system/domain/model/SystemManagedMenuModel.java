package com.cariesguard.system.domain.model;

public record SystemManagedMenuModel(
        Long menuId,
        Long orgId,
        Long parentId,
        String permissionCode) {
}
