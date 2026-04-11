package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemConfigModel;
import com.cariesguard.system.domain.repository.SystemConfigRepository;
import com.cariesguard.system.infrastructure.dataobject.SysConfigDO;
import com.cariesguard.system.infrastructure.mapper.SysConfigMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private final SysConfigMapper sysConfigMapper;

    public SystemConfigRepositoryImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public Optional<SystemConfigModel> findActiveByKey(String configKey, Long orgId) {
        SysConfigDO config = sysConfigMapper.selectOne(Wrappers.<SysConfigDO>lambdaQuery()
                .eq(SysConfigDO::getConfigKey, configKey)
                .eq(SysConfigDO::getOrgId, orgId)
                .eq(SysConfigDO::getStatus, "ACTIVE")
                .eq(SysConfigDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(config).map(this::toModel);
    }

    private SystemConfigModel toModel(SysConfigDO item) {
        return new SystemConfigModel(
                item.getConfigKey(),
                item.getConfigName(),
                item.getConfigValue(),
                item.getValueTypeCode(),
                "1".equals(item.getSensitiveFlag()));
    }
}
