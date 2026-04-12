package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PatientDetailModel;
import com.cariesguard.patient.domain.model.PatientGuardianModel;
import com.cariesguard.patient.domain.repository.PatientQueryRepository;
import com.cariesguard.patient.interfaces.vo.PatientDetailVO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PatientQueryAppServiceTests {

    @Mock
    private PatientQueryRepository patientQueryRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPatientShouldReturnMaskedDetail() {
        PatientQueryAppService appService = new PatientQueryAppService(patientQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(patientQueryRepository.findPatientDetail(3001L)).thenReturn(Optional.of(new PatientDetailModel(
                3001L,
                "PAT202604120001",
                "张*",
                "MALE",
                13,
                "CAMPUS_SCREENING",
                2001L,
                List.of(new PatientGuardianModel("张*", "PARENT", "139****0000", "1")),
                null)));

        PatientDetailVO result = appService.getPatient(3001L);

        assertThat(result.patientId()).isEqualTo(3001L);
        assertThat(result.guardianList()).hasSize(1);
        assertThat(result.patientNameMasked()).isEqualTo("张*");
    }

    @Test
    void getPatientShouldRejectCrossOrgAccess() {
        PatientQueryAppService appService = new PatientQueryAppService(patientQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(patientQueryRepository.findPatientDetail(3001L)).thenReturn(Optional.of(new PatientDetailModel(
                3001L,
                "PAT202604120001",
                "张*",
                "MALE",
                13,
                "CAMPUS_SCREENING",
                9999L,
                List.of(),
                null)));

        assertThatThrownBy(() -> appService.getPatient(3001L))
                .isInstanceOf(BusinessException.class);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
