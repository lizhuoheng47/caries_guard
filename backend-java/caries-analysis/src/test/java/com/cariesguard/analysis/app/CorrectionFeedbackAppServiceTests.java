package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackExportCandidateModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.CorrectionFeedbackDomainService;
import com.cariesguard.analysis.interfaces.command.ExportCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.ReviewCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackExportVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackReviewVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
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
                3001L,
                7001L,
                5001L,
                "RE_GRADE",
                objectMapper().createObjectNode().put("gradingLabel", "C2"),
                objectMapper().createObjectNode().put("gradingLabel", "C1"),
                "C2",
                "C1",
                0.42,
                false,
                "doctor corrected tooth grade",
                true));

        assertThat(result.caseId()).isEqualTo(3001L);
        assertThat(result.originalAiGrade()).isEqualTo("C2");
        assertThat(result.doctorCorrectedGrade()).isEqualTo("C1");
        assertThat(result.acceptedAiConclusion()).isFalse();
        assertThat(result.trainingCandidateFlag()).isEqualTo("1");
        ArgumentCaptor<CorrectionFeedbackCreateModel> captor = ArgumentCaptor.forClass(CorrectionFeedbackCreateModel.class);
        verify(anaCorrectionFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().correctedTruthJson()).contains("feedbackGovernance");
        assertThat(captor.getValue().correctedTruthJson()).contains("feedback-governance-v1");
        assertThat(captor.getValue().correctedTruthJson()).contains("doctor corrected tooth grade");
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

    @Test
    void exportTrainingCandidatesShouldReturnSnapshotAndMarkRows() throws Exception {
        CorrectionFeedbackAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        ObjectMapper mapper = objectMapper();
        ObjectNode correctedTruth = mapper.createObjectNode().put("gradingLabel", "C1");
        correctedTruth.putObject("feedbackGovernance")
                .put("originalAiGrade", "C2")
                .put("doctorCorrectedGrade", "C1")
                .put("originalUncertainty", 0.42)
                .put("acceptedAiConclusion", false)
                .put("correctionReason", "doctor corrected tooth grade")
                .put("schemaVersion", "feedback-governance-v1");
        when(anaCorrectionFeedbackRepository.listTrainingCandidates(2001L, 2)).thenReturn(List.of(
                new CorrectionFeedbackExportCandidateModel(
                        9001L,
                        3001L,
                        7001L,
                        5001L,
                        4001L,
                        1001L,
                        mapper.createObjectNode().put("gradingLabel", "C2").toString(),
                        mapper.writeValueAsString(correctedTruth),
                        "RE_GRADE",
                        "APPROVED",
                        2001L,
                        LocalDateTime.now())));

        CorrectionFeedbackExportVO result = appService.exportTrainingCandidates(new ExportCorrectionFeedbackCommand(2));

        assertThat(result.snapshotNo()).startsWith("FBDS-");
        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.samples().get(0).feedbackId()).isEqualTo(9001L);
        assertThat(result.samples().get(0).originalAiGrade()).isEqualTo("C2");
        assertThat(result.samples().get(0).doctorCorrectedGrade()).isEqualTo("C1");
        assertThat(result.samples().get(0).acceptedAiConclusion()).isFalse();
        assertThat(result.samples().get(0).governanceSchemaVersion()).isEqualTo("feedback-governance-v1");
        verify(anaCorrectionFeedbackRepository).markExported(List.of(9001L), result.snapshotNo());
    }

    @Test
    void reviewShouldUpdateFeedbackStatusForCurrentOrg() {
        CorrectionFeedbackAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "reviewer", "hash", "Reviewer", true, List.of("DOCTOR")));
        when(anaCorrectionFeedbackRepository.reviewFeedbacks(
                eq(List.of(9001L, 9002L)),
                eq("APPROVED"),
                eq("1"),
                eq(1001L),
                any(LocalDateTime.class),
                eq(2001L))).thenReturn(2);

        CorrectionFeedbackReviewVO result = appService.review(new ReviewCorrectionFeedbackCommand(
                Arrays.asList(9001L, 9002L, 9001L, null),
                "approved",
                true));

        assertThat(result.reviewedCount()).isEqualTo(2);
        assertThat(result.reviewStatusCode()).isEqualTo("APPROVED");
        assertThat(result.trainingCandidateFlag()).isEqualTo("1");
        verify(anaCorrectionFeedbackRepository).reviewFeedbacks(
                eq(List.of(9001L, 9002L)),
                eq("APPROVED"),
                eq("1"),
                eq(1001L),
                any(LocalDateTime.class),
                eq(2001L));
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
