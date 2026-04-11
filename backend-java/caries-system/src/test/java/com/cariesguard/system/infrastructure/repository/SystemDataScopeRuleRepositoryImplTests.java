package com.cariesguard.system.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.system.domain.model.SystemDataScopeRuleModel;
import com.cariesguard.system.infrastructure.mapper.SysDataPermissionRuleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemDataScopeRuleRepositoryImplTests {

    @Mock
    private SysDataPermissionRuleMapper sysDataPermissionRuleMapper;

    @Test
    void shouldPreferModuleScopeOverRoleScope() {
        SystemDataScopeRuleRepositoryImpl repository = new SystemDataScopeRuleRepositoryImpl(
                sysDataPermissionRuleMapper,
                new ObjectMapper());
        when(sysDataPermissionRuleMapper.selectModuleScopeTypesByUserIdAndModule(100001L, "SYSTEM"))
                .thenReturn(List.of("SELF", "CUSTOM"));
        when(sysDataPermissionRuleMapper.selectCustomDeptIdsJsonByUserIdAndModule(100001L, "SYSTEM"))
                .thenReturn(List.of("[10,20]"));

        SystemDataScopeRuleModel result = repository.resolveByUserIdAndModule(100001L, "SYSTEM");

        assertThat(result.scopeType()).isEqualTo(DataScopeType.CUSTOM);
        assertThat(result.customDeptIds()).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void shouldFallbackToRoleScopeWhenModuleRuleMissing() {
        SystemDataScopeRuleRepositoryImpl repository = new SystemDataScopeRuleRepositoryImpl(
                sysDataPermissionRuleMapper,
                new ObjectMapper());
        when(sysDataPermissionRuleMapper.selectModuleScopeTypesByUserIdAndModule(100001L, "SYSTEM"))
                .thenReturn(List.of());
        when(sysDataPermissionRuleMapper.selectRoleDataScopesByUserId(100001L))
                .thenReturn(List.of("SELF", "ORG"));

        SystemDataScopeRuleModel result = repository.resolveByUserIdAndModule(100001L, "SYSTEM");

        assertThat(result.scopeType()).isEqualTo(DataScopeType.ORG);
        assertThat(result.customDeptIds()).isEmpty();
    }
}
