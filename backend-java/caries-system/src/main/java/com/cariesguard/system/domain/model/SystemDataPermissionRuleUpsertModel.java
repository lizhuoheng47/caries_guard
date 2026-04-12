package com.cariesguard.system.domain.model;

import java.util.Map;
import java.util.Set;

public record SystemDataPermissionRuleUpsertModel(
        Long ruleId,
        Long roleId,
        String moduleCode,
        String scopeTypeCode,
        Set<Long> customDeptIds,
        Map<String, Object> columnMaskPolicy,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {
}
