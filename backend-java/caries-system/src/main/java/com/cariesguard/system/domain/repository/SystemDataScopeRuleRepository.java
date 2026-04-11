package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemDataScopeRuleModel;

public interface SystemDataScopeRuleRepository {

    SystemDataScopeRuleModel resolveByUserIdAndModule(Long userId, String moduleCode);
}
