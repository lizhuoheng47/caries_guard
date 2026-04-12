package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.CreateVisitCommand;
import com.cariesguard.patient.interfaces.vo.VisitMutationVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class VisitCommandAppServiceTests {

    @Mock
    private VisitCaseCommandRepository visitCaseCommandRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createVisitShouldPersistVisitForOwnedPatient() {
        VisitCommandAppService appService = new VisitCommandAppService(visitCaseCommandRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findPatient(3001L)).thenReturn(Optional.of(new PatientOwnedModel(3001L, 2001L)));

        VisitMutationVO result = appService.createVisit(new CreateVisitCommand(
                3001L,
                10L,
                21L,
                "SCREENING",
                LocalDateTime.of(2026, 4, 12, 9, 0),
                "screening",
                null,
                null,
                null,
                null));

        ArgumentCaptor<VisitCreateModel> captor = ArgumentCaptor.forClass(VisitCreateModel.class);
        verify(visitCaseCommandRepository).createVisit(captor.capture());
        assertThat(result.visitId()).isEqualTo(captor.getValue().visitId());
        assertThat(captor.getValue().orgId()).isEqualTo(2001L);
        assertThat(captor.getValue().visitNo()).startsWith("VIS");
    }

    @Test
    void createVisitShouldRejectCrossOrgPatient() {
        VisitCommandAppService appService = new VisitCommandAppService(visitCaseCommandRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findPatient(3001L)).thenReturn(Optional.of(new PatientOwnedModel(3001L, 9999L)));

        assertThatThrownBy(() -> appService.createVisit(new CreateVisitCommand(
                3001L,
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(BusinessException.class);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
