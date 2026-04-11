package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemManagedUserModel;
import com.cariesguard.system.domain.model.SystemUserUpsertModel;
import java.util.Optional;
import java.util.Set;

public interface SystemUserCommandRepository {

    boolean existsUsername(String username, Long excludeUserId);

    boolean existsUserNo(String userNo, Long excludeUserId);

    boolean existsActiveDept(Long deptId, Long orgId);

    Set<Long> findActiveRoleIds(Set<Long> roleIds, Long orgId);

    Optional<SystemManagedUserModel> findManagedUser(Long userId);

    void createUser(SystemUserUpsertModel model);

    void updateUser(SystemUserUpsertModel model);
}
