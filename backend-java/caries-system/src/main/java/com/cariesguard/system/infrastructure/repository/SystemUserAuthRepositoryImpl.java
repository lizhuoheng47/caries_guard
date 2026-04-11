package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import com.cariesguard.system.infrastructure.dataobject.SysUserDO;
import com.cariesguard.system.infrastructure.mapper.SysRoleMapper;
import com.cariesguard.system.infrastructure.mapper.SysUserMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SystemUserAuthRepositoryImpl implements SystemUserAuthRepository {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    public SystemUserAuthRepositoryImpl(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public Optional<SystemUserAuthModel> findByUsername(String username) {
        SysUserDO user = sysUserMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username)
                .eq(SysUserDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(user).map(this::toModel);
    }

    @Override
    public Optional<SystemUserAuthModel> findByUserId(Long userId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !Long.valueOf(0L).equals(user.getDeletedFlag())) {
            return Optional.empty();
        }
        return Optional.of(toModel(user));
    }

    private SystemUserAuthModel toModel(SysUserDO user) {
        List<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        return new SystemUserAuthModel(
                user.getId(),
                user.getOrgId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRealNameMasked(),
                user.getUserTypeCode(),
                user.getStatus(),
                roleCodes);
    }
}
