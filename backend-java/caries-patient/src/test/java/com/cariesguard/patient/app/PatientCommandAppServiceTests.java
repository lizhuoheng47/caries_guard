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
import com.cariesguard.patient.domain.repository.PatientCommandRepository;
import com.cariesguard.patient.interfaces.command.CreatePatientCommand;
import com.cariesguard.patient.interfaces.command.PatientGuardianCommand;
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
        when(sensitiveDataFacade.protectPhone(null)).thenReturn(new ProtectedValue(null, null, null));
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

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
