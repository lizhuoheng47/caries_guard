package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemManagedMenuModel;
import com.cariesguard.system.domain.model.SystemMenuUpsertModel;
import com.cariesguard.system.domain.repository.SystemMenuCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemMenuCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemMenuCommand;
import com.cariesguard.system.interfaces.vo.SystemMenuMutationVO;
import java.util.List;
import java.util.Optional;
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
class SystemMenuCommandAppServiceTests {

    @Mock
    private SystemMenuCommandRepository systemMenuCommandRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createMenuShouldPersistNormalizedFlags() {
        SystemMenuCommandAppService service = new SystemMenuCommandAppService(systemMenuCommandRepository);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        CreateSystemMenuCommand command = new CreateSystemMenuCommand();
        command.setParentId(0L);
        command.setMenuName("Role Manage");
        command.setMenuTypeCode("MENU");
        command.setPermissionCode("system:role:manage");
        command.setOrderNum(20);

        when(systemMenuCommandRepository.existsPermissionCode("system:role:manage", null)).thenReturn(false);

        SystemMenuMutationVO result = service.createMenu(command);

        ArgumentCaptor<SystemMenuUpsertModel> captor = ArgumentCaptor.forClass(SystemMenuUpsertModel.class);
        verify(systemMenuCommandRepository).createMenu(captor.capture());
        assertThat(captor.getValue().parentId()).isNull();
        assertThat(captor.getValue().visibleFlag()).isEqualTo("1");
        assertThat(captor.getValue().cacheFlag()).isEqualTo("0");
        assertThat(result.permissionCode()).isEqualTo("system:role:manage");
    }

    @Test
    void updateMenuShouldValidateParentWithinOrg() {
        SystemMenuCommandAppService service = new SystemMenuCommandAppService(systemMenuCommandRepository);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        UpdateSystemMenuCommand command = new UpdateSystemMenuCommand();
        command.setParentId(2000L);
        command.setMenuName("Role Manage");
        command.setMenuTypeCode("MENU");
        command.setPermissionCode("system:role:manage");
        command.setOrderNum(20);
        command.setStatus("ACTIVE");

        when(systemMenuCommandRepository.findManagedMenu(3001L))
                .thenReturn(Optional.of(new SystemManagedMenuModel(3001L, 100001L, 0L, "system:role:list")));
        when(systemMenuCommandRepository.existsActiveParentMenu(2000L, 100001L)).thenReturn(true);
        when(systemMenuCommandRepository.existsPermissionCode("system:role:manage", 3001L)).thenReturn(false);

        SystemMenuMutationVO result = service.updateMenu(3001L, command);

        assertThat(result.parentId()).isEqualTo(2000L);
        verify(systemMenuCommandRepository).updateMenu(org.mockito.ArgumentMatchers.any(SystemMenuUpsertModel.class));
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
