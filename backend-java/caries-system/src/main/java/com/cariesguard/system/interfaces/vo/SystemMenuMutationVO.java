package com.cariesguard.system.interfaces.vo;

public record SystemMenuMutationVO(
        Long menuId,
        Long parentId,
        String menuName,
        String permissionCode,
        String status) {
}
