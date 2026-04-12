package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemManagedMenuModel;
import com.cariesguard.system.domain.model.SystemMenuUpsertModel;
import com.cariesguard.system.domain.repository.SystemMenuCommandRepository;
import com.cariesguard.system.infrastructure.dataobject.SysMenuDO;
import com.cariesguard.system.infrastructure.mapper.SysMenuMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SystemMenuCommandRepositoryImpl implements SystemMenuCommandRepository {

    private final SysMenuMapper sysMenuMapper;

    public SystemMenuCommandRepositoryImpl(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    @Override
    public boolean existsPermissionCode(String permissionCode, Long excludeMenuId) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }
        var query = Wrappers.<SysMenuDO>lambdaQuery()
                .eq(SysMenuDO::getPermissionCode, permissionCode)
                .eq(SysMenuDO::getDeletedFlag, 0L);
        if (excludeMenuId != null) {
            query.ne(SysMenuDO::getId, excludeMenuId);
        }
        return sysMenuMapper.selectCount(query) > 0;
    }

    @Override
    public boolean existsActiveParentMenu(Long parentId, Long orgId) {
        return sysMenuMapper.selectCount(Wrappers.<SysMenuDO>lambdaQuery()
                .eq(SysMenuDO::getId, parentId)
                .eq(SysMenuDO::getOrgId, orgId)
                .eq(SysMenuDO::getStatus, "ACTIVE")
                .eq(SysMenuDO::getDeletedFlag, 0L)) > 0;
    }

    @Override
    public Optional<SystemManagedMenuModel> findManagedMenu(Long menuId) {
        SysMenuDO menu = sysMenuMapper.selectOne(Wrappers.<SysMenuDO>lambdaQuery()
                .eq(SysMenuDO::getId, menuId)
                .eq(SysMenuDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (menu == null) {
            return Optional.empty();
        }
        return Optional.of(new SystemManagedMenuModel(
                menu.getId(),
                menu.getOrgId(),
                menu.getParentId(),
                menu.getPermissionCode()));
    }

    @Override
    public void createMenu(SystemMenuUpsertModel model) {
        SysMenuDO menu = toMenuDO(model);
        menu.setCreatedBy(model.operatorUserId());
        sysMenuMapper.insert(menu);
    }

    @Override
    public void updateMenu(SystemMenuUpsertModel model) {
        sysMenuMapper.updateById(toMenuDO(model));
    }

    private SysMenuDO toMenuDO(SystemMenuUpsertModel model) {
        SysMenuDO menu = new SysMenuDO();
        menu.setId(model.menuId());
        menu.setParentId(model.parentId());
        menu.setMenuName(model.menuName());
        menu.setMenuTypeCode(model.menuTypeCode());
        menu.setRoutePath(model.routePath());
        menu.setComponentPath(model.componentPath());
        menu.setPermissionCode(model.permissionCode());
        menu.setIcon(model.icon());
        menu.setVisibleFlag(model.visibleFlag());
        menu.setCacheFlag(model.cacheFlag());
        menu.setOrderNum(model.orderNum());
        menu.setOrgId(model.orgId());
        menu.setStatus(model.status());
        menu.setRemark(model.remark());
        menu.setUpdatedBy(model.operatorUserId());
        menu.setDeletedFlag(0L);
        return menu;
    }
}
