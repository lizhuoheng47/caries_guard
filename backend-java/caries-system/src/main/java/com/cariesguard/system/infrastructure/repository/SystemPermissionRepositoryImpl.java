package com.cariesguard.system.infrastructure.repository;

import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import com.cariesguard.system.infrastructure.mapper.SysMenuMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SystemPermissionRepositoryImpl implements SystemPermissionRepository {

    private final SysMenuMapper sysMenuMapper;

    public SystemPermissionRepositoryImpl(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    @Override
    public List<String> findPermissionCodesByUserId(Long userId) {
        return sysMenuMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    public boolean hasPermissionCode(Long userId, String permissionCode) {
        return sysMenuMapper.countPermissionCodeByUserId(userId, permissionCode) > 0;
    }
}
