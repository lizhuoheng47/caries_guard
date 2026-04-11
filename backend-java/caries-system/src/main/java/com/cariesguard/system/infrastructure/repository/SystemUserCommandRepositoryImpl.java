package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemManagedUserModel;
import com.cariesguard.system.domain.model.SystemUserUpsertModel;
import com.cariesguard.system.domain.repository.SystemUserCommandRepository;
import com.cariesguard.system.infrastructure.dataobject.SysDeptDO;
import com.cariesguard.system.infrastructure.dataobject.SysRoleDO;
import com.cariesguard.system.infrastructure.dataobject.SysUserDO;
import com.cariesguard.system.infrastructure.dataobject.SysUserRoleDO;
import com.cariesguard.system.infrastructure.mapper.SysDeptMapper;
import com.cariesguard.system.infrastructure.mapper.SysRoleMapper;
import com.cariesguard.system.infrastructure.mapper.SysUserMapper;
import com.cariesguard.system.infrastructure.mapper.SysUserRoleMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class SystemUserCommandRepositoryImpl implements SystemUserCommandRepository {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public SystemUserCommandRepositoryImpl(SysUserMapper sysUserMapper,
                                           SysRoleMapper sysRoleMapper,
                                           SysDeptMapper sysDeptMapper,
                                           SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public boolean existsUsername(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username)
                .eq(SysUserDO::getDeletedFlag, 0L);
        if (excludeUserId != null) {
            query.ne(SysUserDO::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(query) > 0;
    }

    @Override
    public boolean existsUserNo(String userNo, Long excludeUserId) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUserNo, userNo)
                .eq(SysUserDO::getDeletedFlag, 0L);
        if (excludeUserId != null) {
            query.ne(SysUserDO::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(query) > 0;
    }

    @Override
    public boolean existsActiveDept(Long deptId, Long orgId) {
        return sysDeptMapper.selectCount(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getId, deptId)
                .eq(SysDeptDO::getOrgId, orgId)
                .eq(SysDeptDO::getStatus, "ACTIVE")
                .eq(SysDeptDO::getDeletedFlag, 0L)) > 0;
    }

    @Override
    public Set<Long> findActiveRoleIds(Set<Long> roleIds, Long orgId) {
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(sysRoleMapper.selectList(Wrappers.<SysRoleDO>lambdaQuery()
                        .in(SysRoleDO::getId, roleIds)
                        .eq(SysRoleDO::getOrgId, orgId)
                        .eq(SysRoleDO::getStatus, "ACTIVE")
                        .eq(SysRoleDO::getDeletedFlag, 0L))
                .stream()
                .map(SysRoleDO::getId)
                .toList());
    }

    @Override
    public Optional<SystemManagedUserModel> findManagedUser(Long userId) {
        SysUserDO user = sysUserMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getId, userId)
                .eq(SysUserDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new SystemManagedUserModel(
                user.getId(),
                user.getOrgId(),
                user.getUserNo(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getStatus(),
                sysUserRoleMapper.selectRoleIdsByUserId(userId)));
    }

    @Override
    public void createUser(SystemUserUpsertModel model) {
        SysUserDO user = toUserDO(model);
        user.setCreatedBy(model.operatorUserId());
        sysUserMapper.insert(user);
        syncUserRoles(model.userId(), model.orgId(), model.operatorUserId(), model.roleIds());
    }

    @Override
    public void updateUser(SystemUserUpsertModel model) {
        sysUserMapper.updateById(toUserDO(model));
        syncUserRoles(model.userId(), model.orgId(), model.operatorUserId(), model.roleIds());
    }

    private void syncUserRoles(Long userId, Long orgId, Long operatorUserId, Set<Long> roleIds) {
        List<SysUserRoleDO> currentRelations = sysUserRoleMapper.selectList(Wrappers.<SysUserRoleDO>lambdaQuery()
                .eq(SysUserRoleDO::getUserId, userId)
                .eq(SysUserRoleDO::getDeletedFlag, 0L));
        Set<Long> currentRoleIds = currentRelations.stream()
                .map(SysUserRoleDO::getRoleId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (SysUserRoleDO relation : currentRelations) {
            if (!roleIds.contains(relation.getRoleId())) {
                SysUserRoleDO update = new SysUserRoleDO();
                update.setId(relation.getId());
                update.setDeletedFlag(relation.getId());
                sysUserRoleMapper.updateById(update);
            }
        }
        for (Long roleId : roleIds) {
            if (currentRoleIds.contains(roleId)) {
                continue;
            }
            SysUserRoleDO relation = new SysUserRoleDO();
            long relationId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
            relation.setId(relationId);
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setOrgId(orgId);
            relation.setDeletedFlag(0L);
            relation.setCreatedBy(operatorUserId);
            sysUserRoleMapper.insert(relation);
        }
    }

    private SysUserDO toUserDO(SystemUserUpsertModel model) {
        SysUserDO user = new SysUserDO();
        user.setId(model.userId());
        user.setDeptId(model.deptId());
        user.setUserNo(model.userNo());
        user.setUsername(model.username());
        user.setPasswordHash(model.passwordHash());
        user.setRealNameEnc(model.realNameEnc());
        user.setRealNameHash(model.realNameHash());
        user.setRealNameMasked(model.realNameMasked());
        user.setNickName(model.nickName());
        user.setUserTypeCode(model.userTypeCode());
        user.setGenderCode(model.genderCode());
        user.setPhoneEnc(model.phoneEnc());
        user.setPhoneHash(model.phoneHash());
        user.setPhoneMasked(model.phoneMasked());
        user.setEmailEnc(model.emailEnc());
        user.setEmailHash(model.emailHash());
        user.setEmailMasked(model.emailMasked());
        user.setAvatarUrl(model.avatarUrl());
        user.setCertificateTypeCode(model.certificateTypeCode());
        user.setCertificateNoEnc(model.certificateNoEnc());
        user.setCertificateNoHash(model.certificateNoHash());
        user.setCertificateNoMasked(model.certificateNoMasked());
        user.setPwdUpdatedAt(model.pwdUpdatedAt());
        user.setOrgId(model.orgId());
        user.setStatus(model.status());
        user.setRemark(model.remark());
        user.setUpdatedBy(model.operatorUserId());
        user.setDeletedFlag(0L);
        return user;
    }
}
