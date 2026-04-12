package com.cariesguard.system.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemManagedRoleModel;
import com.cariesguard.system.domain.model.SystemRoleUpsertModel;
import com.cariesguard.system.domain.repository.SystemRoleCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemRoleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemRoleCommand;
import com.cariesguard.system.interfaces.vo.SystemRoleMutationVO;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemRoleCommandAppService {

    private final SystemRoleCommandRepository systemRoleCommandRepository;

    public SystemRoleCommandAppService(SystemRoleCommandRepository systemRoleCommandRepository) {
        this.systemRoleCommandRepository = systemRoleCommandRepository;
    }

    @Transactional
    public SystemRoleMutationVO createRole(CreateSystemRoleCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        Set<Long> menuIds = normalizeIds(command.getMenuIds());
        validateRoleCode(command.getRoleCode(), null);
        validateMenus(menuIds, operator.getOrgId());

        SystemRoleUpsertModel model = new SystemRoleUpsertModel(
                IdWorker.getId(),
                command.getRoleCode().trim(),
                command.getRoleName().trim(),
                command.getRoleSort(),
                normalizeDataScope(command.getDataScopeCode()),
                "0",
                operator.getOrgId(),
                defaultStatus(command.getStatus()),
                command.getRemark(),
                operator.getUserId(),
                menuIds);
        systemRoleCommandRepository.createRole(model);
        return new SystemRoleMutationVO(model.roleId(), model.roleCode(), model.status(), menuIds.stream().toList());
    }

    @Transactional
    public SystemRoleMutationVO updateRole(Long roleId, UpdateSystemRoleCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        SystemManagedRoleModel existing = systemRoleCommandRepository.findManagedRole(roleId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System role does not exist"));
        if (!existing.orgId().equals(operator.getOrgId()) && !operator.hasAnyRole("ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        Set<Long> menuIds = normalizeIds(command.getMenuIds());
        validateRoleCode(command.getRoleCode(), roleId);
        validateMenus(menuIds, existing.orgId());
        validateBuiltinRole(existing, command);

        SystemRoleUpsertModel model = new SystemRoleUpsertModel(
                roleId,
                command.getRoleCode().trim(),
                command.getRoleName().trim(),
                command.getRoleSort(),
                normalizeDataScope(command.getDataScopeCode()),
                existing.isBuiltin(),
                existing.orgId(),
                command.getStatus(),
                command.getRemark(),
                operator.getUserId(),
                menuIds);
        systemRoleCommandRepository.updateRole(model);
        return new SystemRoleMutationVO(model.roleId(), model.roleCode(), model.status(), menuIds.stream().toList());
    }

    private void validateRoleCode(String roleCode, Long excludeRoleId) {
        if (systemRoleCommandRepository.existsRoleCode(roleCode == null ? null : roleCode.trim(), excludeRoleId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Role code already exists");
        }
    }

    private void validateMenus(Set<Long> menuIds, Long orgId) {
        if (systemRoleCommandRepository.findActiveMenuIds(menuIds, orgId).size() != menuIds.size()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Menu selection contains invalid or inactive records");
        }
    }

    private void validateBuiltinRole(SystemManagedRoleModel existing, UpdateSystemRoleCommand command) {
        if (!"1".equals(existing.isBuiltin())) {
            return;
        }
        if (!existing.roleCode().equals(command.getRoleCode().trim())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Built-in role code cannot be changed");
        }
        if (!"ACTIVE".equals(command.getStatus())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Built-in role cannot be disabled");
        }
    }

    private Set<Long> normalizeIds(java.util.List<Long> ids) {
        return new LinkedHashSet<>(ids);
    }

    private String normalizeDataScope(String dataScopeCode) {
        return StringUtils.hasText(dataScopeCode) ? dataScopeCode.trim() : "SELF";
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "ACTIVE";
    }
}
