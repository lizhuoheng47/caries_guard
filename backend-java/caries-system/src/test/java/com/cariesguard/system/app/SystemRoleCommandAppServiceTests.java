package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemManagedRoleModel;
import com.cariesguard.system.domain.model.SystemRoleUpsertModel;
import com.cariesguard.system.domain.repository.SystemRoleCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemRoleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemRoleCommand;
import com.cariesguard.system.interfaces.vo.SystemRoleMutationVO;
import java.util.List;
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
class SystemRoleCommandAppServiceTests {

    @Mock
    private SystemRoleCommandRepository systemRoleCommandRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRoleShouldPersistMenuBindings() {
        SystemRoleCommandAppService service = new SystemRoleCommandAppService(systemRoleCommandRepository);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        CreateSystemRoleCommand command = new CreateSystemRoleCommand();
        command.setRoleCode("CASE_REVIEWER");
        command.setRoleName("Case Reviewer");
        command.setRoleSort(10);
        command.setDataScopeCode("ORG");
        command.setStatus("ACTIVE");
        command.setMenuIds(List.of(3001L, 3002L));

        when(systemRoleCommandRepository.existsRoleCode("CASE_REVIEWER", null)).thenReturn(false);
        when(systemRoleCommandRepository.findActiveMenuIds(Set.of(3001L, 3002L), 100001L))
                .thenReturn(Set.of(3001L, 3002L));

        SystemRoleMutationVO result = service.createRole(command);

        ArgumentCaptor<SystemRoleUpsertModel> captor = ArgumentCaptor.forClass(SystemRoleUpsertModel.class);
        verify(systemRoleCommandRepository).createRole(captor.capture());
        assertThat(captor.getValue().orgId()).isEqualTo(100001L);
        assertThat(captor.getValue().menuIds()).containsExactlyInAnyOrder(3001L, 3002L);
        assertThat(result.roleCode()).isEqualTo("CASE_REVIEWER");
    }

    @Test
    void updateBuiltInRoleShouldKeepCodeAndStatusActive() {
        SystemRoleCommandAppService service = new SystemRoleCommandAppService(systemRoleCommandRepository);
        prepareAuthenticatedUser(100001L, 100001L, "SYS_ADMIN");

        UpdateSystemRoleCommand command = new UpdateSystemRoleCommand();
        command.setRoleCode("SYS_ADMIN");
        command.setRoleName("System Administrator");
        command.setRoleSort(1);
        command.setDataScopeCode("ALL");
        command.setStatus("ACTIVE");
        command.setMenuIds(List.of(3001L));

        when(systemRoleCommandRepository.findManagedRole(2001L)).thenReturn(Optional.of(
                new SystemManagedRoleModel(2001L, 100001L, "SYS_ADMIN", "1", List.of(3001L))));
        when(systemRoleCommandRepository.existsRoleCode("SYS_ADMIN", 2001L)).thenReturn(false);
        when(systemRoleCommandRepository.findActiveMenuIds(Set.of(3001L), 100001L)).thenReturn(Set.of(3001L));

        SystemRoleMutationVO result = service.updateRole(2001L, command);

        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(systemRoleCommandRepository).updateRole(org.mockito.ArgumentMatchers.any(SystemRoleUpsertModel.class));
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
