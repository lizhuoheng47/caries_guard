package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.DiagnosisItemCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseDiagnosesCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseToothRecordsCommand;
import com.cariesguard.patient.interfaces.command.ToothRecordItemCommand;
import com.cariesguard.patient.interfaces.vo.CaseDiagnosisMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseToothRecordMutationVO;
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
class CaseClinicalRecordAppServiceTests {

    @Mock
    private VisitCaseCommandRepository visitCaseCommandRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveDiagnosesShouldReplaceCaseDiagnoses() {
        CaseClinicalRecordAppService appService = new CaseClinicalRecordAppService(visitCaseCommandRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findManagedCase(6001L))
                .thenReturn(Optional.of(new CaseManagedModel(6001L, "REVIEW_PENDING", "0", "0", 2001L)));

        CaseDiagnosisMutationVO result = appService.saveDiagnoses(6001L, new SaveCaseDiagnosesCommand(List.of(
                new DiagnosisItemCommand("CARIES", "Caries", "MEDIUM", "AI+doctor", "desc", "advise", "1", null))));

        assertThat(result.caseId()).isEqualTo(6001L);
        assertThat(result.diagnosisCount()).isEqualTo(1);
        verify(visitCaseCommandRepository).replaceDiagnoses(any(), any(), any(), any());
    }

    @Test
    void saveToothRecordsShouldRejectDuplicatePayload() {
        CaseClinicalRecordAppService appService = new CaseClinicalRecordAppService(visitCaseCommandRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findManagedCase(6001L))
                .thenReturn(Optional.of(new CaseManagedModel(6001L, "REVIEW_PENDING", "0", "0", 2001L)));

        assertThatThrownBy(() -> appService.saveToothRecords(6001L, new SaveCaseToothRecordsCommand(List.of(
                new ToothRecordItemCommand(7001L, "16", "O", "CARIES", "MEDIUM", "finding", "suggestion", 1, null),
                new ToothRecordItemCommand(7002L, "16", "O", "CARIES", "DEEP", "finding2", "suggestion2", 2, null)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate tooth record");
    }

    @Test
    void saveToothRecordsShouldReplaceCaseToothRecords() {
        CaseClinicalRecordAppService appService = new CaseClinicalRecordAppService(visitCaseCommandRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitCaseCommandRepository.findManagedCase(6001L))
                .thenReturn(Optional.of(new CaseManagedModel(6001L, "REVIEW_PENDING", "0", "0", 2001L)));

        CaseToothRecordMutationVO result = appService.saveToothRecords(6001L, new SaveCaseToothRecordsCommand(List.of(
                new ToothRecordItemCommand(7001L, "16", "O", "CARIES", "MEDIUM", "finding", "suggestion", 1, null))));

        assertThat(result.caseId()).isEqualTo(6001L);
        assertThat(result.toothRecordCount()).isEqualTo(1);
        verify(visitCaseCommandRepository).replaceToothRecords(any(), any(), any());
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
