package com.cariesguard.framework.security.datascope;

import java.util.List;
import java.util.Set;

public record DataScopeContext(
        Long userId,
        Long orgId,
        List<String> roleCodes,
        DataScopeType scopeType,
        Set<Long> customDeptIds) {
}
