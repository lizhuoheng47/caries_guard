package com.cariesguard.system.infrastructure.repository;

import com.cariesguard.system.domain.model.SystemLoginLogModel;
import com.cariesguard.system.domain.repository.SystemLoginLogRepository;
import com.cariesguard.system.infrastructure.dataobject.SysLoginLogDO;
import com.cariesguard.system.infrastructure.mapper.SysLoginLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SystemLoginLogRepositoryImpl implements SystemLoginLogRepository {

    private final SysLoginLogMapper sysLoginLogMapper;

    public SystemLoginLogRepositoryImpl(SysLoginLogMapper sysLoginLogMapper) {
        this.sysLoginLogMapper = sysLoginLogMapper;
    }

    @Override
    public void save(SystemLoginLogModel model) {
        SysLoginLogDO item = new SysLoginLogDO();
        item.setTraceId(model.traceId());
        item.setUsername(model.username());
        item.setUserId(model.userId());
        item.setOrgId(model.orgId());
        item.setLoginStatusCode(model.loginStatusCode());
        item.setClientIp(model.clientIp());
        item.setUserAgent(model.userAgent());
        item.setFailureReason(model.failureReason());
        item.setLoginTime(model.loginTime());
        sysLoginLogMapper.insert(item);
    }
}
