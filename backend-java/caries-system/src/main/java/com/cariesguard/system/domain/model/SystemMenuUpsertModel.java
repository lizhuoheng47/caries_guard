package com.cariesguard.system.domain.model;

public record SystemMenuUpsertModel(
        Long menuId,
        Long parentId,
        String menuName,
        String menuTypeCode,
        String routePath,
        String componentPath,
        String permissionCode,
        String icon,
        String visibleFlag,
        String cacheFlag,
        Integer orderNum,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
