package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemManagedRoleModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleUpsertModel;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SystemRoleCommandRepository {

    boolean existsRoleCode(String roleCode, Long excludeRoleId);

    Set<Long> findActiveMenuIds(Set<Long> menuIds, Long orgId);

    List<SystemMenuSummaryModel> findMenusByIds(Set<Long> menuIds, Long orgId);

    Optional<SystemManagedRoleModel> findManagedRole(Long roleId);

    void createRole(SystemRoleUpsertModel model);

    void updateRole(SystemRoleUpsertModel model);
}
