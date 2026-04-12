package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleSummaryModel;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleUpsertModel;
import com.cariesguard.system.domain.model.SystemManagedDataPermissionRuleModel;
import com.cariesguard.system.domain.repository.SystemDataPermissionRuleManageRepository;
import com.cariesguard.system.infrastructure.dataobject.SysDataPermissionRuleDO;
import com.cariesguard.system.infrastructure.dataobject.SysDeptDO;
import com.cariesguard.system.infrastructure.dataobject.SysRoleDO;
import com.cariesguard.system.infrastructure.mapper.SysDataPermissionRuleMapper;
import com.cariesguard.system.infrastructure.mapper.SysDeptMapper;
import com.cariesguard.system.infrastructure.mapper.SysRoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SystemDataPermissionRuleManageRepositoryImpl implements SystemDataPermissionRuleManageRepository {

    private final SysDataPermissionRuleMapper sysDataPermissionRuleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ObjectMapper objectMapper;

    public SystemDataPermissionRuleManageRepositoryImpl(SysDataPermissionRuleMapper sysDataPermissionRuleMapper,
                                                        SysRoleMapper sysRoleMapper,
                                                        SysDeptMapper sysDeptMapper,
                                                        ObjectMapper objectMapper) {
        this.sysDataPermissionRuleMapper = sysDataPermissionRuleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SystemDataPermissionRuleSummaryModel> listRules(Long roleId, Long orgId, String moduleCode) {
        var query = Wrappers.<SysDataPermissionRuleDO>lambdaQuery()
                .eq(SysDataPermissionRuleDO::getRoleId, roleId)
                .eq(SysDataPermissionRuleDO::getOrgId, orgId)
                .eq(SysDataPermissionRuleDO::getDeletedFlag, 0L)
                .orderByAsc(SysDataPermissionRuleDO::getModuleCode)
                .orderByAsc(SysDataPermissionRuleDO::getScopeTypeCode)
                .orderByAsc(SysDataPermissionRuleDO::getId);
        if (StringUtils.hasText(moduleCode)) {
            query.eq(SysDataPermissionRuleDO::getModuleCode, moduleCode);
        }
        return sysDataPermissionRuleMapper.selectList(query).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public boolean existsDuplicateRule(Long roleId, String moduleCode, String scopeTypeCode, Long excludeRuleId) {
        var query = Wrappers.<SysDataPermissionRuleDO>lambdaQuery()
                .eq(SysDataPermissionRuleDO::getRoleId, roleId)
                .eq(SysDataPermissionRuleDO::getModuleCode, moduleCode)
                .eq(SysDataPermissionRuleDO::getScopeTypeCode, scopeTypeCode)
                .eq(SysDataPermissionRuleDO::getDeletedFlag, 0L);
        if (excludeRuleId != null) {
            query.ne(SysDataPermissionRuleDO::getId, excludeRuleId);
        }
        return sysDataPermissionRuleMapper.selectCount(query) > 0;
    }

    @Override
    public boolean existsActiveRole(Long roleId, Long orgId) {
        return sysRoleMapper.selectCount(Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getId, roleId)
                .eq(SysRoleDO::getOrgId, orgId)
                .eq(SysRoleDO::getStatus, "ACTIVE")
                .eq(SysRoleDO::getDeletedFlag, 0L)) > 0;
    }

    @Override
    public Set<Long> findActiveDeptIds(Set<Long> deptIds, Long orgId) {
        if (deptIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(sysDeptMapper.selectList(Wrappers.<SysDeptDO>lambdaQuery()
                        .in(SysDeptDO::getId, deptIds)
                        .eq(SysDeptDO::getOrgId, orgId)
                        .eq(SysDeptDO::getStatus, "ACTIVE")
                        .eq(SysDeptDO::getDeletedFlag, 0L))
                .stream()
                .map(SysDeptDO::getId)
                .toList());
    }

    @Override
    public Optional<SystemManagedDataPermissionRuleModel> findManagedRule(Long ruleId) {
        SysDataPermissionRuleDO rule = sysDataPermissionRuleMapper.selectOne(Wrappers.<SysDataPermissionRuleDO>lambdaQuery()
                .eq(SysDataPermissionRuleDO::getId, ruleId)
                .eq(SysDataPermissionRuleDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (rule == null) {
            return Optional.empty();
        }
        return Optional.of(new SystemManagedDataPermissionRuleModel(rule.getId(), rule.getRoleId(), rule.getOrgId()));
    }

    @Override
    public void createRule(SystemDataPermissionRuleUpsertModel model) {
        SysDataPermissionRuleDO rule = toDO(model);
        rule.setCreatedBy(model.operatorUserId());
        sysDataPermissionRuleMapper.insert(rule);
    }

    @Override
    public void updateRule(SystemDataPermissionRuleUpsertModel model) {
        sysDataPermissionRuleMapper.updateById(toDO(model));
    }

    private SystemDataPermissionRuleSummaryModel toSummary(SysDataPermissionRuleDO item) {
        return new SystemDataPermissionRuleSummaryModel(
                item.getId(),
                item.getRoleId(),
                item.getModuleCode(),
                item.getScopeTypeCode(),
                parseDeptIds(item.getDeptIdsJson()),
                "1".equals(item.getSelfOnlyFlag()),
                parseMaskPolicy(item.getColumnMaskPolicyJson()),
                item.getOrgId(),
                item.getStatus(),
                item.getRemark());
    }

    private SysDataPermissionRuleDO toDO(SystemDataPermissionRuleUpsertModel model) {
        SysDataPermissionRuleDO rule = new SysDataPermissionRuleDO();
        rule.setId(model.ruleId());
        rule.setRoleId(model.roleId());
        rule.setModuleCode(model.moduleCode());
        rule.setScopeTypeCode(model.scopeTypeCode());
        rule.setDeptIdsJson(writeJson(model.customDeptIds()));
        rule.setSelfOnlyFlag("SELF".equals(model.scopeTypeCode()) ? "1" : "0");
        rule.setColumnMaskPolicyJson(writeJson(model.columnMaskPolicy()));
        rule.setOrgId(model.orgId());
        rule.setStatus(model.status());
        rule.setRemark(model.remark());
        rule.setUpdatedBy(model.operatorUserId());
        rule.setDeletedFlag(0L);
        return rule;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private Set<Long> parseDeptIds(String json) {
        if (!StringUtils.hasText(json)) {
            return Set.of();
        }
        try {
            TreeSet<Long> deptIds = new TreeSet<>(objectMapper.readValue(json, new TypeReference<List<Long>>() { }));
            return Set.copyOf(deptIds);
        } catch (Exception exception) {
            return Set.of();
        }
    }

    private Map<String, Object> parseMaskPolicy(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return Map.copyOf(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { }));
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
