package com.cariesguard.system.domain.model;

import com.cariesguard.framework.security.datascope.DataScopeType;
import java.util.Set;

public record SystemDataScopeRuleModel(
        DataScopeType scopeType,
        Set<Long> customDeptIds) {
}
