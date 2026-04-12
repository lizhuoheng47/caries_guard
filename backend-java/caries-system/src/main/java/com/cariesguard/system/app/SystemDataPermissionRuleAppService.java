package com.cariesguard.system.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleSummaryModel;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleUpsertModel;
import com.cariesguard.system.domain.model.SystemManagedDataPermissionRuleModel;
import com.cariesguard.system.domain.repository.SystemDataPermissionRuleManageRepository;
import com.cariesguard.system.interfaces.command.CreateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.vo.SystemDataPermissionRuleVO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemDataPermissionRuleAppService {

    private final SystemDataPermissionRuleManageRepository systemDataPermissionRuleManageRepository;

    public SystemDataPermissionRuleAppService(SystemDataPermissionRuleManageRepository systemDataPermissionRuleManageRepository) {
        this.systemDataPermissionRuleManageRepository = systemDataPermissionRuleManageRepository;
    }

    public List<SystemDataPermissionRuleVO> listRules(Long roleId, String moduleCode) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        if (!systemDataPermissionRuleManageRepository.existsActiveRole(roleId, operator.getOrgId())
                && !operator.hasAnyRole("ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return systemDataPermissionRuleManageRepository.listRules(
                        roleId,
                        operator.getOrgId(),
                        normalizeModuleCode(moduleCode))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public SystemDataPermissionRuleVO createRule(CreateSystemDataPermissionRuleCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        validateRole(command.getRoleId(), operator.getOrgId());

        String moduleCode = normalizeModuleCode(command.getModuleCode());
        String scopeTypeCode = normalizeScopeType(command.getScopeTypeCode());
        Set<Long> customDeptIds = normalizeCustomDeptIds(scopeTypeCode, command.getCustomDeptIds(), operator.getOrgId());
        validateDuplicate(command.getRoleId(), moduleCode, scopeTypeCode, null);

        SystemDataPermissionRuleUpsertModel model = new SystemDataPermissionRuleUpsertModel(
                IdWorker.getId(),
                command.getRoleId(),
                moduleCode,
                scopeTypeCode,
                customDeptIds,
                normalizeMaskPolicy(command.getColumnMaskPolicy()),
                operator.getOrgId(),
                defaultStatus(command.getStatus()),
                command.getRemark(),
                operator.getUserId());
        systemDataPermissionRuleManageRepository.createRule(model);
        return toVO(model);
    }

    @Transactional
    public SystemDataPermissionRuleVO updateRule(Long ruleId, UpdateSystemDataPermissionRuleCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        SystemManagedDataPermissionRuleModel existing = systemDataPermissionRuleManageRepository.findManagedRule(ruleId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Data permission rule does not exist"));
        if (!existing.orgId().equals(operator.getOrgId()) && !operator.hasAnyRole("ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        validateRole(command.getRoleId(), existing.orgId());
        String moduleCode = normalizeModuleCode(command.getModuleCode());
        String scopeTypeCode = normalizeScopeType(command.getScopeTypeCode());
        Set<Long> customDeptIds = normalizeCustomDeptIds(scopeTypeCode, command.getCustomDeptIds(), existing.orgId());
        validateDuplicate(command.getRoleId(), moduleCode, scopeTypeCode, ruleId);

        SystemDataPermissionRuleUpsertModel model = new SystemDataPermissionRuleUpsertModel(
                ruleId,
                command.getRoleId(),
                moduleCode,
                scopeTypeCode,
                customDeptIds,
                normalizeMaskPolicy(command.getColumnMaskPolicy()),
                existing.orgId(),
                command.getStatus().trim(),
                command.getRemark(),
                operator.getUserId());
        systemDataPermissionRuleManageRepository.updateRule(model);
        return toVO(model);
    }

    private void validateRole(Long roleId, Long orgId) {
        if (!systemDataPermissionRuleManageRepository.existsActiveRole(roleId, orgId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Role does not exist or is inactive");
        }
    }

    private void validateDuplicate(Long roleId, String moduleCode, String scopeTypeCode, Long excludeRuleId) {
        if (systemDataPermissionRuleManageRepository.existsDuplicateRule(roleId, moduleCode, scopeTypeCode, excludeRuleId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Data permission rule already exists");
        }
    }

    private String normalizeModuleCode(String moduleCode) {
        return StringUtils.hasText(moduleCode) ? moduleCode.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeScopeType(String scopeTypeCode) {
        if (!StringUtils.hasText(scopeTypeCode)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Scope type is required");
        }
        String normalized = scopeTypeCode.trim().toUpperCase(Locale.ROOT);
        try {
            DataScopeType.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Unsupported scope type");
        }
    }

    private Set<Long> normalizeCustomDeptIds(String scopeTypeCode, List<Long> customDeptIds, Long orgId) {
        if (!DataScopeType.CUSTOM.name().equals(scopeTypeCode)) {
            return Set.of();
        }
        Set<Long> normalizedIds = new LinkedHashSet<>(Optional.ofNullable(customDeptIds).orElse(List.of()));
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Custom scope requires department ids");
        }
        if (systemDataPermissionRuleManageRepository.findActiveDeptIds(normalizedIds, orgId).size() != normalizedIds.size()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Custom department selection contains invalid or inactive records");
        }
        return Set.copyOf(normalizedIds);
    }

    private Map<String, Object> normalizeMaskPolicy(Map<String, Object> columnMaskPolicy) {
        return columnMaskPolicy == null ? Map.of() : Map.copyOf(columnMaskPolicy);
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "ACTIVE";
    }

    private SystemDataPermissionRuleVO toVO(SystemDataPermissionRuleUpsertModel model) {
        return new SystemDataPermissionRuleVO(
                model.ruleId(),
                model.roleId(),
                model.moduleCode(),
                model.scopeTypeCode(),
                model.customDeptIds(),
                DataScopeType.SELF.name().equals(model.scopeTypeCode()),
                model.columnMaskPolicy(),
                model.orgId(),
                model.status(),
                model.remark());
    }

    private SystemDataPermissionRuleVO toVO(SystemDataPermissionRuleSummaryModel model) {
        return new SystemDataPermissionRuleVO(
                model.ruleId(),
                model.roleId(),
                model.moduleCode(),
                model.scopeTypeCode(),
                model.customDeptIds(),
                model.selfOnly(),
                model.columnMaskPolicy(),
                model.orgId(),
                model.status(),
                model.remark());
    }
}
