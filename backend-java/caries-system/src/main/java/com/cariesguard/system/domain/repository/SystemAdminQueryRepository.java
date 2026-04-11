package com.cariesguard.system.domain.repository;

import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.system.domain.model.PageQueryResult;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleSummaryModel;
import com.cariesguard.system.domain.model.SystemUserSummaryModel;
import java.util.List;

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
}
