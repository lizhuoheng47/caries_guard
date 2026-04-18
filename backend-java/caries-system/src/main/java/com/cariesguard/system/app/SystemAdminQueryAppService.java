package com.cariesguard.system.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.system.domain.model.SystemMenuDetailModel;
import com.cariesguard.system.domain.model.PageQueryResult;
import com.cariesguard.system.domain.model.SystemRoleDetailModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleSummaryModel;
import com.cariesguard.system.domain.model.SystemUserDetailModel;
import com.cariesguard.system.domain.model.SystemUserSummaryModel;
import com.cariesguard.system.domain.repository.SystemAdminQueryRepository;
import com.cariesguard.system.interfaces.vo.SystemMenuDetailVO;
import com.cariesguard.system.interfaces.vo.PageResultVO;
import com.cariesguard.system.interfaces.vo.SystemMenuListItemVO;
import com.cariesguard.system.interfaces.vo.SystemRoleDetailVO;
import com.cariesguard.system.interfaces.vo.SystemRoleListItemVO;
import com.cariesguard.system.interfaces.vo.SystemUserDetailVO;
import com.cariesguard.system.interfaces.vo.SystemUserListItemVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminQueryAppService {

    private static final String MODULE_CODE = "SYSTEM";

    private final SystemAdminQueryRepository systemAdminQueryRepository;
    private final SystemDataScopeService systemDataScopeService;
    private final CompetitionExposureService competitionExposureService;

    public SystemAdminQueryAppService(SystemAdminQueryRepository systemAdminQueryRepository,
                                      SystemDataScopeService systemDataScopeService,
                                      CompetitionExposureService competitionExposureService) {
        this.systemAdminQueryRepository = systemAdminQueryRepository;
        this.systemDataScopeService = systemDataScopeService;
        this.competitionExposureService = competitionExposureService;
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
                .filter(this::isMenuExposed)
                .map(this::toMenuVO)
                .toList();
    }

    public SystemUserDetailVO getUser(Long userId) {
        return systemAdminQueryRepository.findUserDetail(systemDataScopeService.currentScope(MODULE_CODE), userId)
                .map(this::toUserDetailVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System user does not exist"));
    }

    public SystemRoleDetailVO getRole(Long roleId) {
        return systemAdminQueryRepository.findRoleDetail(systemDataScopeService.currentScope(MODULE_CODE), roleId)
                .map(this::toRoleDetailVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System role does not exist"));
    }

    public SystemMenuDetailVO getMenu(Long menuId) {
        return systemAdminQueryRepository.findMenuDetail(systemDataScopeService.currentScope(MODULE_CODE), menuId)
                .filter(this::isMenuExposed)
                .map(this::toMenuDetailVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System menu does not exist"));
    }

    private boolean isMenuExposed(SystemMenuSummaryModel item) {
        return competitionExposureService.isMenuExposed(item.routePath(), item.permissionCode());
    }

    private boolean isMenuExposed(SystemMenuDetailModel item) {
        return competitionExposureService.isMenuExposed(item.routePath(), item.permissionCode());
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

    private SystemUserDetailVO toUserDetailVO(SystemUserDetailModel item) {
        return new SystemUserDetailVO(
                item.userId(),
                item.deptId(),
                item.userNo(),
                item.username(),
                item.nickName(),
                item.realNameMasked(),
                item.phoneMasked(),
                item.emailMasked(),
                item.avatarUrl(),
                item.userTypeCode(),
                item.genderCode(),
                item.certificateTypeCode(),
                item.certificateNoMasked(),
                item.orgId(),
                item.status(),
                item.remark(),
                item.lastLoginAt(),
                item.pwdUpdatedAt(),
                item.roleIds(),
                item.roleCodes());
    }

    private SystemRoleDetailVO toRoleDetailVO(SystemRoleDetailModel item) {
        return new SystemRoleDetailVO(
                item.roleId(),
                item.roleCode(),
                item.roleName(),
                item.roleSort(),
                item.dataScopeCode(),
                item.builtin(),
                item.orgId(),
                item.status(),
                item.remark(),
                item.menuIds());
    }

    private SystemMenuDetailVO toMenuDetailVO(SystemMenuDetailModel item) {
        return new SystemMenuDetailVO(
                item.menuId(),
                item.parentId(),
                item.menuName(),
                item.menuTypeCode(),
                item.routePath(),
                item.componentPath(),
                item.permissionCode(),
                item.icon(),
                item.orderNum(),
                item.visible(),
                item.cache(),
                item.orgId(),
                item.status(),
                item.remark());
    }
}
