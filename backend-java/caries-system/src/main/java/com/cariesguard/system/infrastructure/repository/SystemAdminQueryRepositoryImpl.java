package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.system.domain.model.PageQueryResult;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleSummaryModel;
import com.cariesguard.system.domain.model.SystemUserSummaryModel;
import com.cariesguard.system.domain.repository.SystemAdminQueryRepository;
import com.cariesguard.system.infrastructure.dataobject.SysMenuDO;
import com.cariesguard.system.infrastructure.dataobject.SysRoleDO;
import com.cariesguard.system.infrastructure.dataobject.SysUserDO;
import com.cariesguard.system.infrastructure.mapper.SysMenuMapper;
import com.cariesguard.system.infrastructure.mapper.SysRoleMapper;
import com.cariesguard.system.infrastructure.mapper.SysUserMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SystemAdminQueryRepositoryImpl implements SystemAdminQueryRepository {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    public SystemAdminQueryRepositoryImpl(SysUserMapper sysUserMapper,
                                          SysRoleMapper sysRoleMapper,
                                          SysMenuMapper sysMenuMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
    }

    @Override
    public PageQueryResult<SystemUserSummaryModel> pageUsers(DataScopeContext dataScopeContext,
                                                             int pageNo,
                                                             int pageSize,
                                                             String keyword,
                                                             Long deptId,
                                                             String userTypeCode,
                                                             String status) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<SysUserDO> query = Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getDeletedFlag, 0L)
                .orderByDesc(SysUserDO::getId);
        applyUserFilters(query, dataScopeContext, keyword, deptId, userTypeCode, status);
        long total = sysUserMapper.selectCount(query);

        LambdaQueryWrapper<SysUserDO> pageQuery = Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getDeletedFlag, 0L)
                .orderByDesc(SysUserDO::getId)
                .last("LIMIT " + ((safePageNo - 1) * safePageSize) + "," + safePageSize);
        applyUserFilters(pageQuery, dataScopeContext, keyword, deptId, userTypeCode, status);

        List<SystemUserSummaryModel> records = sysUserMapper.selectList(pageQuery).stream()
                .map(this::toUserSummary)
                .toList();
        return new PageQueryResult<>(records, total, safePageNo, safePageSize);
    }

    @Override
    public List<SystemRoleSummaryModel> listRoles(DataScopeContext dataScopeContext, String status) {
        LambdaQueryWrapper<SysRoleDO> query = Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getDeletedFlag, 0L)
                .orderByAsc(SysRoleDO::getRoleSort);
        applyOrgFilter(query, dataScopeContext, SysRoleDO::getOrgId);
        if (StringUtils.hasText(status)) {
            query.eq(SysRoleDO::getStatus, status);
        }
        return sysRoleMapper.selectList(query).stream()
                .map(this::toRoleSummary)
                .toList();
    }

    @Override
    public List<SystemMenuSummaryModel> listMenus(DataScopeContext dataScopeContext, String status) {
        LambdaQueryWrapper<SysMenuDO> query = Wrappers.<SysMenuDO>lambdaQuery()
                .eq(SysMenuDO::getDeletedFlag, 0L)
                .orderByAsc(SysMenuDO::getParentId)
                .orderByAsc(SysMenuDO::getOrderNum)
                .orderByAsc(SysMenuDO::getId);
        applyOrgFilter(query, dataScopeContext, SysMenuDO::getOrgId);
        if (StringUtils.hasText(status)) {
            query.eq(SysMenuDO::getStatus, status);
        }
        return sysMenuMapper.selectList(query).stream()
                .map(this::toMenuSummary)
                .toList();
    }

    private void applyUserFilters(LambdaQueryWrapper<SysUserDO> query,
                                  DataScopeContext dataScopeContext,
                                  String keyword,
                                  Long deptId,
                                  String userTypeCode,
                                  String status) {
        applyOrgFilter(query, dataScopeContext, SysUserDO::getOrgId);
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(SysUserDO::getUsername, keyword)
                    .or()
                    .like(SysUserDO::getUserNo, keyword)
                    .or()
                    .like(SysUserDO::getNickName, keyword)
                    .or()
                    .like(SysUserDO::getRealNameMasked, keyword));
        }
        if (deptId != null) {
            query.eq(SysUserDO::getDeptId, deptId);
        }
        if (StringUtils.hasText(userTypeCode)) {
            query.eq(SysUserDO::getUserTypeCode, userTypeCode);
        }
        if (StringUtils.hasText(status)) {
            query.eq(SysUserDO::getStatus, status);
        }
    }

    private <T> void applyOrgFilter(LambdaQueryWrapper<T> query,
                                    DataScopeContext dataScopeContext,
                                    com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> orgGetter) {
        if (dataScopeContext.scopeType() != DataScopeType.ALL) {
            query.eq(orgGetter, dataScopeContext.orgId());
        }
    }

    private SystemUserSummaryModel toUserSummary(SysUserDO item) {
        return new SystemUserSummaryModel(
                item.getId(),
                item.getDeptId(),
                item.getUserNo(),
                item.getUsername(),
                item.getNickName(),
                item.getRealNameMasked(),
                item.getPhoneMasked(),
                item.getUserTypeCode(),
                item.getOrgId(),
                item.getStatus(),
                item.getLastLoginAt());
    }

    private SystemRoleSummaryModel toRoleSummary(SysRoleDO item) {
        return new SystemRoleSummaryModel(
                item.getId(),
                item.getRoleCode(),
                item.getRoleName(),
                item.getRoleSort() == null ? 0 : item.getRoleSort(),
                item.getDataScopeCode(),
                "1".equals(item.getIsBuiltin()),
                item.getOrgId(),
                item.getStatus());
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
