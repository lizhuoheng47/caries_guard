package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.CorrectionFeedbackDomainService;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CorrectionFeedbackAppServiceTests {

    @Mock
    private AnalysisCommandRepository analysisCommandRepository;

    @Mock
    private AnaCorrectionFeedbackRepository anaCorrectionFeedbackRepository;

    private CorrectionFeedbackAppService createService() {
        return new CorrectionFeedbackAppService(
                analysisCommandRepository,
                anaCorrectionFeedbackRepository,
                new CorrectionFeedbackDomainService(),
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitShouldSaveFeedback() {
        CorrectionFeedbackAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 2001L, "REVIEW_PENDING")));
        when(analysisCommandRepository.findImage(5001L)).thenReturn(Optional.of(
                new AnalysisImageModel(5001L, 3001L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));

        CorrectionFeedbackVO result = appService.submit(new SubmitCorrectionFeedbackCommand(
                3001L, 7001L, 5001L, "RE_GRADE", null, null));

        assertThat(result.caseId()).isEqualTo(3001L);
        verify(anaCorrectionFeedbackRepository).save(any());
    }

    @Test
    void submitShouldRejectCaseOutsideReview() {
        CorrectionFeedbackAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 2001L, "ANALYZING")));

        assertThatThrownBy(() -> appService.submit(new SubmitCorrectionFeedbackCommand(
                3001L, 7001L, null, "RE_GRADE", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void submitShouldRejectCrossOrgAccess() {
        CorrectionFeedbackAppService appService = createService();
        // User belongs to org 200001, case belongs to org 100001
        setCurrentUser(new AuthenticatedUser(1001L, 200001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "REVIEW_PENDING")));

        assertThatThrownBy(() -> appService.submit(new SubmitCorrectionFeedbackCommand(
                3001L, 7001L, null, "RE_GRADE", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void submitShouldRejectSourceImageNotInCase() {
        CorrectionFeedbackAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 2001L, "REVIEW_PENDING")));
        when(analysisCommandRepository.findImage(5001L)).thenReturn(Optional.of(
                new AnalysisImageModel(5001L, 9999L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));

        assertThatThrownBy(() -> appService.submit(new SubmitCorrectionFeedbackCommand(
                3001L, 7001L, 5001L, "RE_GRADE", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
