package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.ProtectedValue;
import com.cariesguard.framework.security.sensitive.SensitiveDataFacade;
import com.cariesguard.patient.domain.model.PatientCreateModel;
import com.cariesguard.patient.domain.model.PatientManagedModel;
import com.cariesguard.patient.domain.repository.PatientCommandRepository;
import com.cariesguard.patient.interfaces.command.CreatePatientCommand;
import com.cariesguard.patient.interfaces.command.PatientGuardianCommand;
import com.cariesguard.patient.interfaces.command.UpdatePatientCommand;
import com.cariesguard.patient.interfaces.vo.PatientMutationVO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PatientCommandAppServiceTests {

    @Mock
    private PatientCommandRepository patientCommandRepository;

    @Mock
    private SensitiveDataFacade sensitiveDataFacade;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPatientShouldProtectSensitiveFieldsAndPersist() {
        PatientCommandAppService appService = new PatientCommandAppService(patientCommandRepository, sensitiveDataFacade);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(sensitiveDataFacade.protectName("张三")).thenReturn(new ProtectedValue("enc-name", "hash-name", "张*"));
        when(sensitiveDataFacade.protectBirthDate("2012-06-01")).thenReturn(new ProtectedValue("enc-birth", "hash-birth", "2012-06-**"));
        when(sensitiveDataFacade.protectPhone("13800000000")).thenReturn(new ProtectedValue("enc-phone", "hash-phone", "138****0000"));
        when(sensitiveDataFacade.protectIdCard("440100201206010011")).thenReturn(new ProtectedValue("enc-id", "hash-id", "4401********0011"));
        when(sensitiveDataFacade.protectName("张父")).thenReturn(new ProtectedValue("enc-guardian", "hash-guardian", "张*"));
        when(sensitiveDataFacade.protectPhone("13900000000")).thenReturn(new ProtectedValue("enc-guardian-phone", "hash-guardian-phone", "139****0000"));
        when(sensitiveDataFacade.protectIdCard("440100198001010099")).thenReturn(new ProtectedValue("enc-guardian-id", "hash-guardian-id", "4401********0099"));

        PatientMutationVO result = appService.createPatient(new CreatePatientCommand(
                "张三",
                "MALE",
                LocalDate.of(2012, 6, 1),
                "13800000000",
                "440100201206010011",
                "CAMPUS_SCREENING",
                null,
                "L4",
                "ACTIVE",
                "first record",
                new PatientGuardianCommand("张父", "PARENT", "13900000000", "ID_CARD", "440100198001010099", "1", "ACTIVE", null)));

        ArgumentCaptor<PatientCreateModel> captor = ArgumentCaptor.forClass(PatientCreateModel.class);
        verify(patientCommandRepository).createPatient(captor.capture());
        PatientCreateModel persisted = captor.getValue();
        assertThat(result.patientId()).isEqualTo(persisted.patientId());
        assertThat(result.patientNo()).startsWith("PAT");
        assertThat(persisted.patientNameEnc()).isEqualTo("enc-name");
        assertThat(persisted.idCardHash()).isEqualTo("hash-id");
        assertThat(persisted.orgId()).isEqualTo(2001L);
        assertThat(persisted.guardians()).hasSize(1);
        assertThat(persisted.guardians().get(0).guardianNameMasked()).isEqualTo("张*");
    }

    @Test
    void createPatientShouldRejectDuplicateIdCard() {
        PatientCommandAppService appService = new PatientCommandAppService(patientCommandRepository, sensitiveDataFacade);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(sensitiveDataFacade.protectName("张三")).thenReturn(new ProtectedValue("enc-name", "hash-name", "张*"));
        when(sensitiveDataFacade.protectIdCard("440100201206010011")).thenReturn(new ProtectedValue("enc-id", "hash-id", "4401********0011"));
        when(patientCommandRepository.existsPatientByIdCardHash(2001L, "hash-id")).thenReturn(true);

        assertThatThrownBy(() -> appService.createPatient(new CreatePatientCommand(
                "张三",
                null,
                null,
                null,
                "440100201206010011",
                null,
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Patient id card already exists");
    }

    @Test
    void updatePatientShouldReplaceSensitiveFieldsAndGuardians() {
        PatientCommandAppService appService = new PatientCommandAppService(patientCommandRepository, sensitiveDataFacade);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(patientCommandRepository.findManagedPatient(3001L))
                .thenReturn(java.util.Optional.of(new PatientManagedModel(3001L, "PAT202604120001", "old-hash", 2001L)));
        when(sensitiveDataFacade.protectName("李四")).thenReturn(new ProtectedValue("enc-name", "hash-name", "李*"));
        when(sensitiveDataFacade.protectBirthDate("2013-07-02")).thenReturn(new ProtectedValue("enc-birth", "hash-birth", "2013-07-**"));
        when(sensitiveDataFacade.protectPhone("13700000000")).thenReturn(new ProtectedValue("enc-phone", "hash-phone", "137****0000"));
        when(sensitiveDataFacade.protectIdCard("440100201307020022")).thenReturn(new ProtectedValue("enc-id", "hash-id", "4401********0022"));
        when(sensitiveDataFacade.protectName("李母")).thenReturn(new ProtectedValue("enc-guardian", "hash-guardian", "李*"));
        when(sensitiveDataFacade.protectPhone("13600000000")).thenReturn(new ProtectedValue("enc-guardian-phone", "hash-guardian-phone", "136****0000"));
        when(sensitiveDataFacade.protectIdCard("440100198601010066")).thenReturn(new ProtectedValue("enc-guardian-id", "hash-guardian-id", "4401********0066"));

        PatientMutationVO result = appService.updatePatient(3001L, new UpdatePatientCommand(
                "李四",
                "FEMALE",
                LocalDate.of(2013, 7, 2),
                "13700000000",
                "440100201307020022",
                "OUTPATIENT",
                LocalDate.of(2026, 4, 12),
                "L3",
                "ACTIVE",
                "updated",
                new PatientGuardianCommand("李母", "PARENT", "13600000000", "ID_CARD", "440100198601010066", "1", "ACTIVE", null)));

        ArgumentCaptor<com.cariesguard.patient.domain.model.PatientUpdateModel> captor =
                ArgumentCaptor.forClass(com.cariesguard.patient.domain.model.PatientUpdateModel.class);
        verify(patientCommandRepository).updatePatient(captor.capture());
        assertThat(result.patientId()).isEqualTo(3001L);
        assertThat(result.patientNo()).isEqualTo("PAT202604120001");
        assertThat(captor.getValue().patientNameMasked()).isEqualTo("李*");
        assertThat(captor.getValue().guardians()).hasSize(1);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
