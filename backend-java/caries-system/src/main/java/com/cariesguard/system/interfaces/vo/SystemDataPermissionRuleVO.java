package com.cariesguard.system.interfaces.vo;

import java.util.Map;
import java.util.Set;

public record SystemDataPermissionRuleVO(
        Long ruleId,
        Long roleId,
        String moduleCode,
        String scopeTypeCode,
        Set<Long> customDeptIds,
        boolean selfOnly,
        Map<String, Object> columnMaskPolicy,
        Long orgId,
        String status,
        String remark) {
}
