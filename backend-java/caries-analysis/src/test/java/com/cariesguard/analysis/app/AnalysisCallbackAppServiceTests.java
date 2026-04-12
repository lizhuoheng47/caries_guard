package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.domain.repository.MedRiskAssessmentRecordRepository;
import com.cariesguard.analysis.domain.service.AnalysisCallbackDomainService;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.infrastructure.client.AiCallbackSignatureVerifier;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisCallbackAppServiceTests {

    @Mock
    private AiCallbackSignatureVerifier aiCallbackSignatureVerifier;

    @Mock
    private AnaTaskRecordRepository anaTaskRecordRepository;

    @Mock
    private AnaResultSummaryRepository anaResultSummaryRepository;

    @Mock
    private AnaVisualAssetRepository anaVisualAssetRepository;

    @Mock
    private MedRiskAssessmentRecordRepository medRiskAssessmentRecordRepository;

    @Mock
    private CaseCommandAppService caseCommandAppService;

    @Mock
    private AnalysisTaskEventPublisher analysisTaskEventPublisher;

    private AnalysisCallbackAppService createService() {
        return new AnalysisCallbackAppService(
                aiCallbackSignatureVerifier,
                anaTaskRecordRepository,
                anaResultSummaryRepository,
                anaVisualAssetRepository,
                medRiskAssessmentRecordRepository,
                new AnalysisIdempotencyDomainService(anaTaskRecordRepository),
                new AnalysisCallbackDomainService(),
                analysisTaskEventPublisher,
                caseCommandAppService,
                new ObjectMapper());
    }

    @Test
    void handleSuccessCallbackShouldPersistAndTransition() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "summary":{"overallHighestSeverity":"C2","uncertaintyScore":0.18,"reviewSuggestedFlag":"1","teethCount":3},
                  "visualAssets":[{"assetTypeCode":"OVERLAY","attachmentId":4101}],
                  "riskAssessment":{"overallRiskLevelCode":"HIGH","assessmentReportJson":{"score":88},"recommendedCycleDays":15}
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.taskStatusCode()).isEqualTo("SUCCESS");
        verify(aiCallbackSignatureVerifier).verify(body, "1710000000", "sig");
        verify(anaTaskRecordRepository).updateStatus(any());
        verify(anaResultSummaryRepository).save(any());
        verify(anaVisualAssetRepository).replaceByTaskId(eq(6001L), any());
        verify(medRiskAssessmentRecordRepository).save(any());
        verify(caseCommandAppService).transitionStatusAsSystem(eq(3001L), eq(100001L), any());
        verify(analysisTaskEventPublisher).publishCompleted(any());
    }

    @Test
    void handleDuplicateSuccessCallbackShouldReturnIdempotentAck() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 100001L, null)));

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "summary":{"overallHighestSeverity":"C2"}
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.idempotent()).isTrue();
        verify(anaResultSummaryRepository, never()).save(any());
        verify(caseCommandAppService, never()).transitionStatusAsSystem(eq(3001L), eq(100001L), any());
    }

    @Test
    void handleFailureCallbackShouldRecordError() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "PROCESSING", null,
                        LocalDateTime.now(), LocalDateTime.now(), null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"FAILED",
                  "errorMessage":"model timeout"
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.taskStatusCode()).isEqualTo("FAILED");
        verify(anaTaskRecordRepository).updateStatus(any());
        verify(caseCommandAppService).transitionStatusAsSystem(eq(3001L), eq(100001L), any());
        verify(analysisTaskEventPublisher).publishFailed(any());
    }

    @Test
    void handleDuplicateFailureCallbackShouldReturnIdempotent() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "FAILED", "timeout",
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 100001L, null)));

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"FAILED",
                  "errorMessage":"timeout"
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.idempotent()).isTrue();
        verify(caseCommandAppService, never()).transitionStatusAsSystem(any(), any(), any());
    }

    @Test
    void handleConflictCallbackShouldThrow() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 100001L, null)));

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"FAILED",
                  "errorMessage":"late failure"
                }
                """;
        assertThatThrownBy(() -> appService.handleResultCallback(body, "1710000000", "sig"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void handleLateCallbackOnRetriedTaskShouldSkipWriteBack() {
        AnalysisCallbackAppService appService = createService();
        // Task is in QUEUEING (not terminal), but has already been retried
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(true);

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "summary":{"overallHighestSeverity":"C1"}
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        // Should skip write-back, return idempotent ACK
        assertThat(result.idempotent()).isTrue();
        verify(anaResultSummaryRepository, never()).save(any());
        verify(caseCommandAppService, never()).transitionStatusAsSystem(any(), any(), any());
        verify(analysisTaskEventPublisher, never()).publishCompleted(any());
    }

    @Test
    void handleLowercaseStatusShouldBeAcceptedAfterNormalization() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"success",
                  "summary":{"overallHighestSeverity":"C1"}
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.taskStatusCode()).isEqualTo("SUCCESS");
        verify(anaTaskRecordRepository).updateStatus(any());
        verify(anaResultSummaryRepository).save(any());
    }

    @Test
    void handleBlankTaskStatusShouldThrowValidationError() {
        AnalysisCallbackAppService appService = createService();

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"  "
                }
                """;
        assertThatThrownBy(() -> appService.handleResultCallback(body, "1710000000", "sig"))
                .isInstanceOf(BusinessException.class);
    }
}
