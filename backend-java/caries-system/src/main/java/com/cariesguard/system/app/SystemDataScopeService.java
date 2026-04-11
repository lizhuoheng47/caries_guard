package com.cariesguard.system.app;

import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemDataScopeRuleModel;
import com.cariesguard.system.domain.repository.SystemDataScopeRuleRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemDataScopeService {

    private final SystemDataScopeRuleRepository systemDataScopeRuleRepository;

    public SystemDataScopeService(SystemDataScopeRuleRepository systemDataScopeRuleRepository) {
        this.systemDataScopeRuleRepository = systemDataScopeRuleRepository;
    }

    public DataScopeContext currentScope(String moduleCode) {
        AuthenticatedUser currentUser = SecurityContextUtils.currentUser();
        SystemDataScopeRuleModel rule = systemDataScopeRuleRepository.resolveByUserIdAndModule(
                currentUser.getUserId(),
                moduleCode);
        return new DataScopeContext(
                currentUser.getUserId(),
                currentUser.getOrgId(),
                currentUser.getRoleCodes(),
                rule.scopeType(),
                rule.customDeptIds());
    }
}
