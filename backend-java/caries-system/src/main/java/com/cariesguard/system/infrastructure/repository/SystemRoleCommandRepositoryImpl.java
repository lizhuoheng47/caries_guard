package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemManagedRoleModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleUpsertModel;
import com.cariesguard.system.domain.repository.SystemRoleCommandRepository;
import com.cariesguard.system.infrastructure.dataobject.SysMenuDO;
import com.cariesguard.system.infrastructure.dataobject.SysRoleDO;
import com.cariesguard.system.infrastructure.dataobject.SysRoleMenuDO;
import com.cariesguard.system.infrastructure.mapper.SysMenuMapper;
import com.cariesguard.system.infrastructure.mapper.SysRoleMapper;
import com.cariesguard.system.infrastructure.mapper.SysRoleMenuMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SystemRoleCommandRepositoryImpl implements SystemRoleCommandRepository {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    public SystemRoleCommandRepositoryImpl(SysRoleMapper sysRoleMapper,
                                           SysMenuMapper sysMenuMapper,
                                           SysRoleMenuMapper sysRoleMenuMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
    }

    @Override
    public boolean existsRoleCode(String roleCode, Long excludeRoleId) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        var query = Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getRoleCode, roleCode)
                .eq(SysRoleDO::getDeletedFlag, 0L);
        if (excludeRoleId != null) {
            query.ne(SysRoleDO::getId, excludeRoleId);
        }
        return sysRoleMapper.selectCount(query) > 0;
    }

    @Override
    public Set<Long> findActiveMenuIds(Set<Long> menuIds, Long orgId) {
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(sysMenuMapper.selectList(Wrappers.<SysMenuDO>lambdaQuery()
                        .in(SysMenuDO::getId, menuIds)
                        .eq(SysMenuDO::getOrgId, orgId)
                        .eq(SysMenuDO::getStatus, "ACTIVE")
                        .eq(SysMenuDO::getDeletedFlag, 0L))
                .stream()
                .map(SysMenuDO::getId)
                .toList());
    }

    @Override
    public List<SystemMenuSummaryModel> findMenusByIds(Set<Long> menuIds, Long orgId) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(Wrappers.<SysMenuDO>lambdaQuery()
                        .in(SysMenuDO::getId, menuIds)
                        .eq(SysMenuDO::getOrgId, orgId)
                        .eq(SysMenuDO::getDeletedFlag, 0L)
                        .orderByAsc(SysMenuDO::getParentId)
                        .orderByAsc(SysMenuDO::getOrderNum)
                        .orderByAsc(SysMenuDO::getId))
                .stream()
                .map(this::toMenuSummary)
                .toList();
    }

    @Override
    public Optional<SystemManagedRoleModel> findManagedRole(Long roleId) {
        SysRoleDO role = sysRoleMapper.selectOne(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getId, roleId)
                .eq(SysRoleDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (role == null) {
            return Optional.empty();
        }
        return Optional.of(new SystemManagedRoleModel(
                role.getId(),
                role.getOrgId(),
                role.getRoleCode(),
                role.getIsBuiltin(),
                sysRoleMenuMapper.selectMenuIdsByRoleId(roleId)));
    }

    @Override
    public void createRole(SystemRoleUpsertModel model) {
        SysRoleDO role = toRoleDO(model);
        role.setCreatedBy(model.operatorUserId());
        sysRoleMapper.insert(role);
        syncRoleMenus(model.roleId(), model.orgId(), model.operatorUserId(), model.menuIds());
    }

    @Override
    public void updateRole(SystemRoleUpsertModel model) {
        sysRoleMapper.updateById(toRoleDO(model));
        syncRoleMenus(model.roleId(), model.orgId(), model.operatorUserId(), model.menuIds());
    }

    private void syncRoleMenus(Long roleId, Long orgId, Long operatorUserId, Set<Long> menuIds) {
        List<SysRoleMenuDO> currentRelations = sysRoleMenuMapper.selectList(Wrappers.<SysRoleMenuDO>lambdaQuery()
                .eq(SysRoleMenuDO::getRoleId, roleId)
                .eq(SysRoleMenuDO::getDeletedFlag, 0L));
        Set<Long> currentMenuIds = currentRelations.stream()
                .map(SysRoleMenuDO::getMenuId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (SysRoleMenuDO relation : currentRelations) {
            if (!menuIds.contains(relation.getMenuId())) {
                SysRoleMenuDO update = new SysRoleMenuDO();
                update.setId(relation.getId());
                update.setDeletedFlag(relation.getId());
                sysRoleMenuMapper.updateById(update);
            }
        }

        for (Long menuId : menuIds) {
            if (currentMenuIds.contains(menuId)) {
                continue;
            }
            SysRoleMenuDO relation = new SysRoleMenuDO();
            relation.setId(IdWorker.getId());
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            relation.setOrgId(orgId);
            relation.setDeletedFlag(0L);
            relation.setCreatedBy(operatorUserId);
            sysRoleMenuMapper.insert(relation);
        }
    }

    private SysRoleDO toRoleDO(SystemRoleUpsertModel model) {
        SysRoleDO role = new SysRoleDO();
        role.setId(model.roleId());
        role.setRoleCode(model.roleCode());
        role.setRoleName(model.roleName());
        role.setRoleSort(model.roleSort());
        role.setDataScopeCode(model.dataScopeCode());
        role.setIsBuiltin(model.isBuiltin());
        role.setOrgId(model.orgId());
        role.setStatus(model.status());
        role.setRemark(model.remark());
        role.setUpdatedBy(model.operatorUserId());
        role.setDeletedFlag(0L);
        return role;
    }

    private SystemMenuSummaryModel toMenuSummary(SysMenuDO item) {
        return new SystemMenuSummaryModel(
                item.getId(),
                item.getParentId(),
                item.getMenuName(),
                item.getMenuTypeCode(),
                item.getRoutePath(),
                item.getComponentPath(),
                item.getPermissionCode(),
                item.getOrderNum() == null ? 0 : item.getOrderNum(),
                "1".equals(item.getVisibleFlag()),
                "1".equals(item.getCacheFlag()),
                item.getOrgId(),
                item.getStatus());
    }
}
