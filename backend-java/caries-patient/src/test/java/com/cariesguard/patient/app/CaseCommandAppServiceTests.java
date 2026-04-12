package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.cariesguard.patient.interfaces.command.CreateCaseCommand;
import com.cariesguard.patient.interfaces.vo.CaseMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseStatusTransitionVO;
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
class CaseCommandAppServiceTests {

    @Mock
    private VisitCaseCommandRepository visitCaseCommandRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCaseShouldPersistInitialStatusLog() {
        CaseCommandAppService appService = new CaseCommandAppService(visitCaseCommandRepository, new CaseStatusMachine());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findVisit(4001L)).thenReturn(Optional.of(new VisitOwnedModel(4001L, 3001L, 21L, 2001L)));

        CaseMutationVO result = appService.createCase(new CreateCaseCommand(
                4001L,
                3001L,
                "CARIES_SCREENING",
                "screening case",
                "screening",
                "NORMAL",
                null,
                null,
                null,
                null));

        ArgumentCaptor<CaseCreateModel> caseCaptor = ArgumentCaptor.forClass(CaseCreateModel.class);
        ArgumentCaptor<CaseStatusLogCreateModel> logCaptor = ArgumentCaptor.forClass(CaseStatusLogCreateModel.class);
        verify(visitCaseCommandRepository).createCase(caseCaptor.capture(), logCaptor.capture());
        assertThat(result.caseId()).isEqualTo(caseCaptor.getValue().caseId());
        assertThat(result.caseStatusCode()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().toStatusCode()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().changeReasonCode()).isEqualTo("CASE_CREATED");
    }

    @Test
    void transitionShouldRejectMissingAnalyzePrecondition() {
        CaseCommandAppService appService = new CaseCommandAppService(visitCaseCommandRepository, new CaseStatusMachine());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findManagedCase(5001L))
                .thenReturn(Optional.of(new CaseManagedModel(5001L, "QC_PENDING", "0", "0", 2001L)));
        when(visitCaseCommandRepository.hasActiveImage(5001L)).thenReturn(false);

        assertThatThrownBy(() -> appService.transitionStatus(5001L, new CaseStatusTransitionCommand(
                "ANALYZING",
                "QC_PASSED",
                "pass qc")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active image");
    }

    @Test
    void transitionShouldUpdateCaseAndWriteStatusLog() {
        CaseCommandAppService appService = new CaseCommandAppService(visitCaseCommandRepository, new CaseStatusMachine());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findManagedCase(5001L))
                .thenReturn(Optional.of(new CaseManagedModel(5001L, "REVIEW_PENDING", "0", "0", 2001L)));

        CaseStatusTransitionVO result = appService.transitionStatus(5001L, new CaseStatusTransitionCommand(
                "REPORT_READY",
                "DOCTOR_CONFIRMED",
                "confirmed"));

        assertThat(result.toStatusCode()).isEqualTo("REPORT_READY");
        verify(visitCaseCommandRepository).updateCaseStatus(org.mockito.ArgumentMatchers.any());
        verify(visitCaseCommandRepository).appendCaseStatusLog(org.mockito.ArgumentMatchers.any());
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
