package com.cariesguard.system.infrastructure.repository;

import com.cariesguard.system.domain.model.SystemOperLogModel;
import com.cariesguard.system.domain.repository.SystemOperLogRepository;
import com.cariesguard.system.infrastructure.dataobject.SysOperLogDO;
import com.cariesguard.system.infrastructure.mapper.SysOperLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SystemOperLogRepositoryImpl implements SystemOperLogRepository {

    private final SysOperLogMapper sysOperLogMapper;

    public SystemOperLogRepositoryImpl(SysOperLogMapper sysOperLogMapper) {
        this.sysOperLogMapper = sysOperLogMapper;
    }

    @Override
    public void save(SystemOperLogModel model) {
        SysOperLogDO item = new SysOperLogDO();
        item.setTraceId(model.traceId());
        item.setModuleCode(model.moduleCode());
        item.setOperationTypeCode(model.operationTypeCode());
        item.setOperationName(model.operationName());
        item.setRequestPath(model.requestPath());
        item.setRequestMethod(model.requestMethod());
        item.setTargetId(model.targetId());
        item.setOperatorUserId(model.operatorUserId());
        item.setOrgId(model.orgId());
        item.setSuccessFlag(model.success() ? "1" : "0");
        item.setResultCode(model.resultCode());
        item.setErrorMessage(model.errorMessage());
        item.setOperationTime(model.operationTime());
        sysOperLogMapper.insert(item);
    }
}
