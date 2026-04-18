package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.client.RagClient;
import com.cariesguard.report.domain.model.RagAnswerModel;
import com.cariesguard.report.domain.model.RagAskRequestModel;
import com.cariesguard.report.domain.model.RagCitationModel;
import com.cariesguard.report.domain.model.RagDoctorQaRequestModel;
import com.cariesguard.report.domain.model.RagPatientExplanationRequestModel;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.interfaces.command.DoctorQaCommand;
import com.cariesguard.report.interfaces.command.PatientExplanationCommand;
import com.cariesguard.report.interfaces.command.RagAskCommand;
import com.cariesguard.report.interfaces.vo.RagAnswerVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RagAppServiceTests {

    @Mock
    private RagClient ragClient;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doctorQaShouldCallPythonRagClientAndReturnCitations() {
        setCurrentUser();
        when(ragClient.doctorQa(any())).thenReturn(new RagAnswerModel(
                "RAG-1",
                "RAGREQ-1",
                "Use fluoride and review occlusal findings.",
                List.of(new RagCitationModel(1, 10L, "Guide", 20L, 0.91, "kb://guide", "evidence")),
                "v1.0",
                "template-llm-v1",
                "0",
                12,
                false));

        RagAppService appService = new RagAppService(ragClient);
        RagAnswerVO answer = appService.doctorQa(new DoctorQaCommand(
                "How should I explain C2 risk?",
                "caries-default",
                3,
                "CASE-1",
                "PAT-1",
                Map.of("toothCode", "16")));

        assertThat(answer.fallback()).isFalse();
        assertThat(answer.answerText()).contains("fluoride");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.safetyFlag()).isEqualTo("0");
        ArgumentCaptor<RagDoctorQaRequestModel> requestCaptor = ArgumentCaptor.forClass(RagDoctorQaRequestModel.class);
        org.mockito.Mockito.verify(ragClient).doctorQa(requestCaptor.capture());
        assertThat(requestCaptor.getValue().javaUserId()).isEqualTo(1001L);
        assertThat(requestCaptor.getValue().orgId()).isEqualTo(2001L);
        assertThat(requestCaptor.getValue().topK()).isEqualTo(3);
    }

    @Test
    void patientExplanationShouldReturnFallbackWhenRagClientFails() {
        setCurrentUser();
        when(ragClient.patientExplanation(any())).thenThrow(new BusinessException(
                CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(),
                "timeout"));

        RagAppService appService = new RagAppService(ragClient);
        RagAnswerVO answer = appService.patientExplanation(new PatientExplanationCommand(
                "Explain this report",
                null,
                null,
                "RPT-1",
                null,
                Map.of("riskLevelCode", "HIGH"),
                "HIGH"));

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.citations()).isEmpty();
        assertThat(answer.safetyFlag()).isEqualTo("1");
        assertThat(answer.safetyFlags()).contains("INSUFFICIENT_EVIDENCE");
    }

    @Test
    void patientReportExplanationShouldPassStructuredCaseSummary() {
        setCurrentUser();
        when(ragClient.patientExplanation(any())).thenReturn(new RagAnswerModel(
                "RAG-2",
                "RAGREQ-2",
                "Patient-facing explanation.",
                List.of(),
                "v1.0",
                "template-llm-v1",
                "0",
                15,
                false));

        RagAppService appService = new RagAppService(ragClient);
        String explanation = appService.generatePatientReportExplanation("RPT-1", new ReportRenderDataModel(
                "CASE-1",
                3001L,
                4001L,
                "PATIENT",
                List.of(),
                List.of(new ReportToothRecordModel(1L, 2L, "16", "OCCLUSAL", "CARIES", "C2", "lesion", "review", 0)),
                List.of(),
                "C2",
                new BigDecimal("0.18"),
                2,
                1,
                "HIGH",
                30,
                "1",
                List.of(),
                null,
                null,
                LocalDateTime.now()));

        assertThat(explanation).isEqualTo("Patient-facing explanation.");
        ArgumentCaptor<RagPatientExplanationRequestModel> requestCaptor =
                ArgumentCaptor.forClass(RagPatientExplanationRequestModel.class);
        org.mockito.Mockito.verify(ragClient).patientExplanation(requestCaptor.capture());
        assertThat(requestCaptor.getValue().relatedBizNo()).isEqualTo("RPT-1");
        assertThat(requestCaptor.getValue().caseSummary()).containsEntry("riskLevelCode", "HIGH");
        assertThat(requestCaptor.getValue().caseSummary()).containsKey("toothFindings");
    }

    @Test
    void askShouldCallUnifiedPythonRagEndpoint() {
        setCurrentUser();
        when(ragClient.ask(any())).thenReturn(new RagAnswerModel(
                "RAG-3",
                "RAGREQ-3",
                "Unified answer.",
                List.of(new RagCitationModel(1, "caries-default", "DOC-1", "v1.0", 10L,
                        "Guide", 20L, 0.91, 0.91, "kb://guide", "evidence")),
                "v1.0",
                "template-llm-v1",
                "0",
                List.of("MEDICAL_CAUTION"),
                null,
                0.82,
                "trace-1",
                12,
                false));

        RagAppService appService = new RagAppService(ragClient);
        RagAnswerVO answer = appService.ask(new RagAskCommand(
                "How should I explain C2 risk?",
                "DOCTOR_QA",
                "caries-default",
                3,
                "CASE-1",
                "PAT-1",
                Map.of("riskLevelCode", "MEDIUM")));

        assertThat(answer.confidence()).isEqualTo(0.82);
        assertThat(answer.safetyFlags()).containsExactly("MEDICAL_CAUTION");
        assertThat(answer.citations().get(0).documentCode()).isEqualTo("DOC-1");
        ArgumentCaptor<RagAskRequestModel> requestCaptor = ArgumentCaptor.forClass(RagAskRequestModel.class);
        org.mockito.Mockito.verify(ragClient).ask(requestCaptor.capture());
        assertThat(requestCaptor.getValue().javaUserId()).isEqualTo(1001L);
        assertThat(requestCaptor.getValue().orgId()).isEqualTo(2001L);
        assertThat(requestCaptor.getValue().scene()).isEqualTo("DOCTOR_QA");
    }

    private void setCurrentUser() {
        AuthenticatedUser user = new AuthenticatedUser(
                1001L,
                2001L,
                "doctor",
                "hash",
                "Doctor",
                true,
                List.of("DOCTOR"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
