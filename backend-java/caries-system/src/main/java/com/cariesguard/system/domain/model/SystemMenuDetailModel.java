package com.cariesguard.system.domain.model;

public record SystemMenuDetailModel(
        Long menuId,
        Long parentId,
        String menuName,
        String menuTypeCode,
        String routePath,
        String componentPath,
        String permissionCode,
        String icon,
        int orderNum,
        boolean visible,
        boolean cache,
        Long orgId,
        String status,
        String remark) {
}
