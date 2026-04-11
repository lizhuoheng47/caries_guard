package com.cariesguard.system.infrastructure.repository;

import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.system.domain.model.SystemDataScopeRuleModel;
import com.cariesguard.system.domain.repository.SystemDataScopeRuleRepository;
import com.cariesguard.system.infrastructure.mapper.SysDataPermissionRuleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SystemDataScopeRuleRepositoryImpl implements SystemDataScopeRuleRepository {

    private final SysDataPermissionRuleMapper sysDataPermissionRuleMapper;
    private final ObjectMapper objectMapper;

    public SystemDataScopeRuleRepositoryImpl(SysDataPermissionRuleMapper sysDataPermissionRuleMapper,
                                             ObjectMapper objectMapper) {
        this.sysDataPermissionRuleMapper = sysDataPermissionRuleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public SystemDataScopeRuleModel resolveByUserIdAndModule(Long userId, String moduleCode) {
        DataScopeType scopeType = sysDataPermissionRuleMapper.selectRoleDataScopesByUserId(userId).stream()
                .map(this::toScopeType)
                .max(Comparator.comparingInt(this::priority))
                .orElse(DataScopeType.SELF);
        Set<Long> customDeptIds = scopeType == DataScopeType.CUSTOM
                ? parseDeptIds(sysDataPermissionRuleMapper.selectCustomDeptIdsJsonByUserIdAndModule(userId, moduleCode))
                : Set.of();
        return new SystemDataScopeRuleModel(scopeType, customDeptIds);
    }

    private DataScopeType toScopeType(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return DataScopeType.SELF;
        }
        try {
            return DataScopeType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DataScopeType.SELF;
        }
    }

    private int priority(DataScopeType scopeType) {
        return switch (scopeType) {
            case SELF -> 1;
            case CUSTOM -> 2;
            case DEPT -> 3;
            case ORG -> 4;
            case ALL -> 5;
        };
    }

    private Set<Long> parseDeptIds(List<String> deptIdsJsonList) {
        TreeSet<Long> deptIds = new TreeSet<>();
        for (String json : deptIdsJsonList) {
            if (!StringUtils.hasText(json)) {
                continue;
            }
            try {
                deptIds.addAll(objectMapper.readValue(json, new TypeReference<List<Long>>() { }));
            } catch (Exception ignored) {
                // Ignore malformed custom data scope payloads and fall back to an empty custom set.
            }
        }
        return Set.copyOf(deptIds);
    }
}
