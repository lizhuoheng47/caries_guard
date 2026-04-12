package com.cariesguard.system.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemManagedMenuModel;
import com.cariesguard.system.domain.model.SystemMenuUpsertModel;
import com.cariesguard.system.domain.repository.SystemMenuCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemMenuCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemMenuCommand;
import com.cariesguard.system.interfaces.vo.SystemMenuMutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemMenuCommandAppService {

    private final SystemMenuCommandRepository systemMenuCommandRepository;

    public SystemMenuCommandAppService(SystemMenuCommandRepository systemMenuCommandRepository) {
        this.systemMenuCommandRepository = systemMenuCommandRepository;
    }

    @Transactional
    public SystemMenuMutationVO createMenu(CreateSystemMenuCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        Long parentId = normalizeParentId(command.getParentId());
        validateParent(parentId, operator.getOrgId(), null);
        validatePermissionCode(command.getPermissionCode(), null);

        SystemMenuUpsertModel model = new SystemMenuUpsertModel(
                IdWorker.getId(),
                parentId,
                command.getMenuName().trim(),
                command.getMenuTypeCode().trim(),
                command.getRoutePath(),
                command.getComponentPath(),
                normalizePermissionCode(command.getPermissionCode()),
                command.getIcon(),
                defaultFlag(command.getVisibleFlag(), "1"),
                defaultFlag(command.getCacheFlag(), "0"),
                command.getOrderNum(),
                operator.getOrgId(),
                defaultStatus(command.getStatus()),
                command.getRemark(),
                operator.getUserId());
        systemMenuCommandRepository.createMenu(model);
        return new SystemMenuMutationVO(
                model.menuId(),
                model.parentId(),
                model.menuName(),
                model.permissionCode(),
                model.status());
    }

    @Transactional
    public SystemMenuMutationVO updateMenu(Long menuId, UpdateSystemMenuCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        SystemManagedMenuModel existing = systemMenuCommandRepository.findManagedMenu(menuId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System menu does not exist"));
        if (!existing.orgId().equals(operator.getOrgId()) && !operator.hasAnyRole("ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        Long parentId = normalizeParentId(command.getParentId());
        validateParent(parentId, existing.orgId(), menuId);
        validatePermissionCode(command.getPermissionCode(), menuId);

        SystemMenuUpsertModel model = new SystemMenuUpsertModel(
                menuId,
                parentId,
                command.getMenuName().trim(),
                command.getMenuTypeCode().trim(),
                command.getRoutePath(),
                command.getComponentPath(),
                normalizePermissionCode(command.getPermissionCode()),
                command.getIcon(),
                defaultFlag(command.getVisibleFlag(), "1"),
                defaultFlag(command.getCacheFlag(), "0"),
                command.getOrderNum(),
                existing.orgId(),
                command.getStatus(),
                command.getRemark(),
                operator.getUserId());
        systemMenuCommandRepository.updateMenu(model);
        return new SystemMenuMutationVO(
                model.menuId(),
                model.parentId(),
                model.menuName(),
                model.permissionCode(),
                model.status());
    }

    private void validateParent(Long parentId, Long orgId, Long currentMenuId) {
        if (parentId == null) {
            return;
        }
        if (currentMenuId != null && currentMenuId.equals(parentId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Menu parent cannot reference itself");
        }
        if (!systemMenuCommandRepository.existsActiveParentMenu(parentId, orgId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Parent menu does not exist or is inactive");
        }
    }

    private void validatePermissionCode(String permissionCode, Long excludeMenuId) {
        String normalized = normalizePermissionCode(permissionCode);
        if (systemMenuCommandRepository.existsPermissionCode(normalized, excludeMenuId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Permission code already exists");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }

    private String normalizePermissionCode(String permissionCode) {
        return StringUtils.hasText(permissionCode) ? permissionCode.trim() : null;
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "ACTIVE";
    }

    private String defaultFlag(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
