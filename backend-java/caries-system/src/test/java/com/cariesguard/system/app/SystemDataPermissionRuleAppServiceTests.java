package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemDataPermissionRuleUpsertModel;
import com.cariesguard.system.domain.model.SystemManagedDataPermissionRuleModel;
import com.cariesguard.system.domain.repository.SystemDataPermissionRuleManageRepository;
import com.cariesguard.system.interfaces.command.CreateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.vo.SystemDataPermissionRuleVO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SystemDataPermissionRuleAppServiceTests {

    @Mock
    private SystemDataPermissionRuleManageRepository systemDataPermissionRuleManageRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRuleShouldPersistCustomDeptScope() {
        SystemDataPermissionRuleAppService service = new SystemDataPermissionRuleAppService(systemDataPermissionRuleManageRepository);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        CreateSystemDataPermissionRuleCommand command = new CreateSystemDataPermissionRuleCommand();
        command.setRoleId(2001L);
        command.setModuleCode("patient");
        command.setScopeTypeCode("custom");
        command.setCustomDeptIds(List.of(3001L, 3002L));
        command.setColumnMaskPolicy(Map.of("phone", "MASK"));

        when(systemDataPermissionRuleManageRepository.existsActiveRole(2001L, 100001L)).thenReturn(true);
        when(systemDataPermissionRuleManageRepository.findActiveDeptIds(Set.of(3001L, 3002L), 100001L))
                .thenReturn(Set.of(3001L, 3002L));
        when(systemDataPermissionRuleManageRepository.existsDuplicateRule(2001L, "PATIENT", "CUSTOM", null))
                .thenReturn(false);

        SystemDataPermissionRuleVO result = service.createRule(command);

        ArgumentCaptor<SystemDataPermissionRuleUpsertModel> captor =
                ArgumentCaptor.forClass(SystemDataPermissionRuleUpsertModel.class);
        verify(systemDataPermissionRuleManageRepository).createRule(captor.capture());
        assertThat(captor.getValue().moduleCode()).isEqualTo("PATIENT");
        assertThat(captor.getValue().scopeTypeCode()).isEqualTo("CUSTOM");
        assertThat(captor.getValue().customDeptIds()).containsExactlyInAnyOrder(3001L, 3002L);
        assertThat(result.columnMaskPolicy()).containsEntry("phone", "MASK");
    }

    @Test
    void createRuleShouldRejectEmptyCustomDeptScope() {
        SystemDataPermissionRuleAppService service = new SystemDataPermissionRuleAppService(systemDataPermissionRuleManageRepository);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        CreateSystemDataPermissionRuleCommand command = new CreateSystemDataPermissionRuleCommand();
        command.setRoleId(2001L);
        command.setModuleCode("CASE");
        command.setScopeTypeCode("CUSTOM");

        when(systemDataPermissionRuleManageRepository.existsActiveRole(2001L, 100001L)).thenReturn(true);

        assertThatThrownBy(() -> service.createRule(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Custom scope requires department ids");
    }

    @Test
    void updateRuleShouldRespectExistingOrg() {
        SystemDataPermissionRuleAppService service = new SystemDataPermissionRuleAppService(systemDataPermissionRuleManageRepository);
        prepareAuthenticatedUser(100001L, 100001L, "SYS_ADMIN");

        UpdateSystemDataPermissionRuleCommand command = new UpdateSystemDataPermissionRuleCommand();
        command.setRoleId(2001L);
        command.setModuleCode("REPORT");
        command.setScopeTypeCode("ORG");
        command.setStatus("ACTIVE");

        when(systemDataPermissionRuleManageRepository.findManagedRule(4001L))
                .thenReturn(Optional.of(new SystemManagedDataPermissionRuleModel(4001L, 2001L, 100001L)));
        when(systemDataPermissionRuleManageRepository.existsActiveRole(2001L, 100001L)).thenReturn(true);
        when(systemDataPermissionRuleManageRepository.existsDuplicateRule(2001L, "REPORT", "ORG", 4001L))
                .thenReturn(false);

        SystemDataPermissionRuleVO result = service.updateRule(4001L, command);

        assertThat(result.orgId()).isEqualTo(100001L);
        verify(systemDataPermissionRuleManageRepository).updateRule(org.mockito.ArgumentMatchers.any(SystemDataPermissionRuleUpsertModel.class));
    }

    private void prepareAuthenticatedUser(Long userId, Long orgId, String roleCode) {
        AuthenticatedUser principal = new AuthenticatedUser(
                userId,
                orgId,
                "admin",
                "hash",
                "Admin",
                true,
                List.of(roleCode));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
