package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemDataPermissionRuleSummaryModel;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleUpsertModel;
import com.cariesguard.system.domain.model.SystemManagedDataPermissionRuleModel;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SystemDataPermissionRuleManageRepository {

    List<SystemDataPermissionRuleSummaryModel> listRules(Long roleId, Long orgId, String moduleCode);

    boolean existsDuplicateRule(Long roleId, String moduleCode, String scopeTypeCode, Long excludeRuleId);

    boolean existsActiveRole(Long roleId, Long orgId);

    Set<Long> findActiveDeptIds(Set<Long> deptIds, Long orgId);

    Optional<SystemManagedDataPermissionRuleModel> findManagedRule(Long ruleId);

    void createRule(SystemDataPermissionRuleUpsertModel model);

    void updateRule(SystemDataPermissionRuleUpsertModel model);
}
