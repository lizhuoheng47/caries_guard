package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.ProtectedValue;
import com.cariesguard.framework.security.sensitive.SensitiveDataFacade;
import com.cariesguard.system.domain.model.SystemManagedUserModel;
import com.cariesguard.system.domain.model.SystemUserUpsertModel;
import com.cariesguard.system.domain.repository.SystemUserCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemUserCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemUserCommand;
import com.cariesguard.system.interfaces.vo.SystemUserMutationVO;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SystemUserCommandAppServiceTests {

    @Mock
    private SystemUserCommandRepository systemUserCommandRepository;

    @Mock
    private SensitiveDataFacade sensitiveDataFacade;

    @Mock
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUserShouldProtectSensitiveFieldsAndPersistRoles() {
        SystemUserCommandAppService service = new SystemUserCommandAppService(
                systemUserCommandRepository,
                sensitiveDataFacade,
                passwordEncoder);
        prepareAuthenticatedUser(100001L, 100001L, "ORG_ADMIN");

        CreateSystemUserCommand command = new CreateSystemUserCommand();
        command.setDeptId(100010L);
        command.setUsername("doctor.a");
        command.setPassword("secret");
        command.setRealName("Alice");
        command.setNickName("Dr Alice");
        command.setUserTypeCode("DOCTOR");
        command.setGenderCode("FEMALE");
        command.setPhone("13800000000");
        command.setEmail("alice@example.com");
        command.setCertificateNo("440101200001010022");
        command.setStatus("ACTIVE");
        command.setRoleIds(List.of(2001L, 2002L));

        when(systemUserCommandRepository.existsActiveDept(100010L, 100001L)).thenReturn(true);
        when(systemUserCommandRepository.findActiveRoleIds(Set.of(2001L, 2002L), 100001L)).thenReturn(Set.of(2001L, 2002L));
        when(systemUserCommandRepository.existsUsername("doctor.a", null)).thenReturn(false);
        when(systemUserCommandRepository.existsUserNo(any(), eq(null))).thenReturn(false);
        when(sensitiveDataFacade.protectName("Alice")).thenReturn(new ProtectedValue("enc-name", "hash-name", "A***"));
        when(sensitiveDataFacade.protectPhone("13800000000")).thenReturn(new ProtectedValue("enc-phone", "hash-phone", "138****0000"));
        when(sensitiveDataFacade.protectGeneric("alice@example.com")).thenReturn(new ProtectedValue("enc-email", "hash-email", "a***"));
        when(sensitiveDataFacade.protectIdCard("440101200001010022")).thenReturn(new ProtectedValue("enc-id", "hash-id", "4401****0022"));
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt");

        SystemUserMutationVO result = service.createUser(command);

        ArgumentCaptor<SystemUserUpsertModel> captor = ArgumentCaptor.forClass(SystemUserUpsertModel.class);
        verify(systemUserCommandRepository).createUser(captor.capture());
        SystemUserUpsertModel model = captor.getValue();
        assertThat(model.orgId()).isEqualTo(100001L);
        assertThat(model.passwordHash()).isEqualTo("bcrypt");
        assertThat(model.realNameEnc()).isEqualTo("enc-name");
        assertThat(model.phoneHash()).isEqualTo("hash-phone");
        assertThat(model.roleIds()).containsExactlyInAnyOrder(2001L, 2002L);
        assertThat(result.username()).isEqualTo("doctor.a");
        assertThat(result.userNo()).startsWith("U");
    }

    @Test
    void updateUserWithoutPasswordShouldKeepOldPasswordHash() {
        SystemUserCommandAppService service = new SystemUserCommandAppService(
                systemUserCommandRepository,
                sensitiveDataFacade,
                passwordEncoder);
        prepareAuthenticatedUser(1L, 100001L, "ORG_ADMIN");

        UpdateSystemUserCommand command = new UpdateSystemUserCommand();
        command.setDeptId(100010L);
        command.setUserNo("U10001");
        command.setUsername("doctor.a");
        command.setRealName("Alice");
        command.setNickName("Doctor Alice");
        command.setUserTypeCode("DOCTOR");
        command.setGenderCode("FEMALE");
        command.setPhone("13800000000");
        command.setEmail("alice@example.com");
        command.setCertificateNo("440101200001010022");
        command.setStatus("ACTIVE");
        command.setRoleIds(List.of(2001L));

        when(systemUserCommandRepository.findManagedUser(3001L)).thenReturn(Optional.of(
                new SystemManagedUserModel(3001L, 100001L, "U10001", "doctor.a", "old-bcrypt", "ACTIVE", List.of(2001L))));
        when(systemUserCommandRepository.existsActiveDept(100010L, 100001L)).thenReturn(true);
        when(systemUserCommandRepository.findActiveRoleIds(Set.of(2001L), 100001L)).thenReturn(Set.of(2001L));
        when(systemUserCommandRepository.existsUsername("doctor.a", 3001L)).thenReturn(false);
        when(systemUserCommandRepository.existsUserNo("U10001", 3001L)).thenReturn(false);
        when(sensitiveDataFacade.protectName("Alice")).thenReturn(new ProtectedValue("enc-name", "hash-name", "A***"));
        when(sensitiveDataFacade.protectPhone("13800000000")).thenReturn(new ProtectedValue("enc-phone", "hash-phone", "138****0000"));
        when(sensitiveDataFacade.protectGeneric("alice@example.com")).thenReturn(new ProtectedValue("enc-email", "hash-email", "a***"));
        when(sensitiveDataFacade.protectIdCard("440101200001010022")).thenReturn(new ProtectedValue("enc-id", "hash-id", "4401****0022"));

        SystemUserMutationVO result = service.updateUser(3001L, command);

        ArgumentCaptor<SystemUserUpsertModel> captor = ArgumentCaptor.forClass(SystemUserUpsertModel.class);
        verify(systemUserCommandRepository).updateUser(captor.capture());
        verify(passwordEncoder, never()).encode(any());
        assertThat(captor.getValue().passwordHash()).isEqualTo("old-bcrypt");
        assertThat(captor.getValue().orgId()).isEqualTo(100001L);
        assertThat(result.roleIds()).containsExactly(2001L);
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
