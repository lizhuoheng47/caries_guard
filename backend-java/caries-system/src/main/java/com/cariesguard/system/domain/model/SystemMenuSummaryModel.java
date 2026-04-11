package com.cariesguard.system.domain.model;

public record SystemMenuSummaryModel(
        Long menuId,
        Long parentId,
        String menuName,
        String menuTypeCode,
        String routePath,
        String componentPath,
        String permissionCode,
        int orderNum,
        boolean visible,
        boolean cache,
        Long orgId,
        String status) {
}
