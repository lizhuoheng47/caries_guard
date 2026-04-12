package com.cariesguard.system.interfaces.vo;

public record SystemMenuDetailVO(
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
