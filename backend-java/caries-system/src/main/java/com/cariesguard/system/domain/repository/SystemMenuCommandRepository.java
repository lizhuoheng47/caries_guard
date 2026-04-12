package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.SystemManagedMenuModel;
import com.cariesguard.system.domain.model.SystemMenuUpsertModel;
import java.util.Optional;

public interface SystemMenuCommandRepository {

    boolean existsPermissionCode(String permissionCode, Long excludeMenuId);

    boolean existsActiveParentMenu(Long parentId, Long orgId);

    Optional<SystemManagedMenuModel> findManagedMenu(Long menuId);

    void createMenu(SystemMenuUpsertModel model);

    void updateMenu(SystemMenuUpsertModel model);
}
