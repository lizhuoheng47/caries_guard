package com.cariesguard.system.domain.repository;

import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.system.domain.model.PageQueryResult;
import com.cariesguard.system.domain.model.SystemMenuDetailModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleDetailModel;
import com.cariesguard.system.domain.model.SystemRoleSummaryModel;
import com.cariesguard.system.domain.model.SystemUserDetailModel;
import com.cariesguard.system.domain.model.SystemUserSummaryModel;
import java.util.List;
import java.util.Optional;

public interface SystemAdminQueryRepository {

    PageQueryResult<SystemUserSummaryModel> pageUsers(
            DataScopeContext dataScopeContext,
            int pageNo,
            int pageSize,
            String keyword,
            Long deptId,
            String userTypeCode,
            String status);

    List<SystemRoleSummaryModel> listRoles(DataScopeContext dataScopeContext, String status);

    List<SystemMenuSummaryModel> listMenus(DataScopeContext dataScopeContext, String status);

    Optional<SystemUserDetailModel> findUserDetail(DataScopeContext dataScopeContext, Long userId);

    Optional<SystemRoleDetailModel> findRoleDetail(DataScopeContext dataScopeContext, Long roleId);

    Optional<SystemMenuDetailModel> findMenuDetail(DataScopeContext dataScopeContext, Long menuId);
}
