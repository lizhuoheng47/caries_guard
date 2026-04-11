package com.cariesguard.system.app;

import com.cariesguard.system.domain.model.PageQueryResult;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleSummaryModel;
import com.cariesguard.system.domain.model.SystemUserSummaryModel;
import com.cariesguard.system.domain.repository.SystemAdminQueryRepository;
import com.cariesguard.system.interfaces.vo.PageResultVO;
import com.cariesguard.system.interfaces.vo.SystemMenuListItemVO;
import com.cariesguard.system.interfaces.vo.SystemRoleListItemVO;
import com.cariesguard.system.interfaces.vo.SystemUserListItemVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminQueryAppService {

    private static final String MODULE_CODE = "SYSTEM";

    private final SystemAdminQueryRepository systemAdminQueryRepository;
    private final SystemDataScopeService systemDataScopeService;

    public SystemAdminQueryAppService(SystemAdminQueryRepository systemAdminQueryRepository,
                                      SystemDataScopeService systemDataScopeService) {
        this.systemAdminQueryRepository = systemAdminQueryRepository;
        this.systemDataScopeService = systemDataScopeService;
    }

    public PageResultVO<SystemUserListItemVO> pageUsers(int pageNo,
                                                        int pageSize,
                                                        String keyword,
                                                        Long deptId,
                                                        String userTypeCode,
                                                        String status) {
        PageQueryResult<SystemUserSummaryModel> result = systemAdminQueryRepository.pageUsers(
                systemDataScopeService.currentScope(MODULE_CODE),
                pageNo,
                pageSize,
                keyword,
                deptId,
                userTypeCode,
                status);
        return new PageResultVO<>(
                result.records().stream().map(this::toUserVO).toList(),
                result.total(),
                result.pageNo(),
                result.pageSize());
    }

    public List<SystemRoleListItemVO> listRoles(String status) {
        return systemAdminQueryRepository.listRoles(systemDataScopeService.currentScope(MODULE_CODE), status).stream()
                .map(this::toRoleVO)
                .toList();
    }

    public List<SystemMenuListItemVO> listMenus(String status) {
        return systemAdminQueryRepository.listMenus(systemDataScopeService.currentScope(MODULE_CODE), status).stream()
                .map(this::toMenuVO)
                .toList();
    }

    private SystemUserListItemVO toUserVO(SystemUserSummaryModel item) {
        return new SystemUserListItemVO(
                item.userId(),
                item.deptId(),
                item.userNo(),
                item.username(),
                item.nickName(),
                item.realNameMasked(),
                item.phoneMasked(),
                item.userTypeCode(),
                item.orgId(),
                item.status(),
                item.lastLoginAt());
    }

    private SystemRoleListItemVO toRoleVO(SystemRoleSummaryModel item) {
        return new SystemRoleListItemVO(
                item.roleId(),
                item.roleCode(),
                item.roleName(),
                item.roleSort(),
                item.dataScopeCode(),
                item.builtin(),
                item.orgId(),
                item.status());
    }

    private SystemMenuListItemVO toMenuVO(SystemMenuSummaryModel item) {
        return new SystemMenuListItemVO(
                item.menuId(),
                item.parentId(),
                item.menuName(),
                item.menuTypeCode(),
                item.routePath(),
                item.componentPath(),
                item.permissionCode(),
                item.orderNum(),
                item.visible(),
                item.cache(),
                item.orgId(),
                item.status());
    }
}
