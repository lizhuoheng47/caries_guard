package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
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
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.domain.model.AttachmentObjectRegistrationModel;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private AttachmentAppService attachmentAppService;

    @Mock
    private AnalysisCommandRepository analysisCommandRepository;

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
                null,
                null,
                new ObjectMapper());
    }

    private AnalysisCallbackAppService createServiceWithAttachmentRegistration() {
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
                attachmentAppService,
                analysisCommandRepository,
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
    void handleSuccessCallbackShouldDeriveAggregatesFromGradingAndNeedsReviewWhenSummaryMissing() {
        AnalysisCallbackAppService appService = createService();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "gradingLabel":"C3",
                  "needsReview":true,
                  "uncertaintyScore":0.67,
                  "rawResultJson":{
                    "gradingLabel":"C3",
                    "confidenceScore":0.58,
                    "uncertaintyScore":0.67,
                    "needsReview":true,
                    "visualAssets":[]
                  }
                }
                """;

        appService.handleResultCallback(body, "1710000000", "sig");

        ArgumentCaptor<AnalysisResultSummaryModel> summaryCaptor = ArgumentCaptor.forClass(AnalysisResultSummaryModel.class);
        verify(anaResultSummaryRepository).save(summaryCaptor.capture());
        AnalysisResultSummaryModel saved = summaryCaptor.getValue();
        assertThat(saved.overallHighestSeverity()).isEqualTo("C3");
        assertThat(saved.reviewSuggestedFlag()).isEqualTo("1");
        assertThat(saved.uncertaintyScore()).isNotNull();
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

    @Test
    void handleSuccessCallbackWithMetadataAssetsShouldRegisterAttachments() {
        AnalysisCallbackAppService appService = createServiceWithAttachmentRegistration();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE-2024-0001", 2001L, 100001L, "AI_PROCESSING")));
        when(analysisCommandRepository.findImage(7001L)).thenReturn(Optional.of(
                new AnalysisImageModel(7001L, 3001L, 8001L, "PERIAPICAL", "PASS", "caries-image", "case/2024/0001/img.png")));
        when(attachmentAppService.registerExternalObject(any())).thenAnswer(invocation -> {
            AttachmentObjectRegistrationModel model = invocation.getArgument(0);
            return new AttachmentUploadVO(9001L, model.originalName(), model.bucketName(), model.objectKey(), model.md5());
        });

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "modelVersion":"caries-v2",
                  "summary":{"overallHighestSeverity":"C2","uncertaintyScore":0.18,"reviewSuggestedFlag":"1","teethCount":3},
                  "visualAssets":[{
                    "assetTypeCode":"OVERLAY",
                    "bucketName":"caries-visual",
                    "objectKey":"org/100001/case/CASE-2024-0001/analysis/TASK1/caries-v2/OVERLAY/7001/16/tmp-abc.png",
                    "contentType":"image/png",
                    "relatedImageId":7001,
                    "toothCode":"16",
                    "fileSizeBytes":2048,
                    "md5":"d41d8cd98f00b204e9800998ecf8427e"
                  }]
                }
                """;
        AnalysisCallbackAckVO result = appService.handleResultCallback(body, "1710000000", "sig");

        assertThat(result.taskStatusCode()).isEqualTo("SUCCESS");
        ArgumentCaptor<AttachmentObjectRegistrationModel> registrationCaptor =
                ArgumentCaptor.forClass(AttachmentObjectRegistrationModel.class);
        verify(attachmentAppService).registerExternalObject(registrationCaptor.capture());
        AttachmentObjectRegistrationModel registered = registrationCaptor.getValue();
        assertThat(registered.bizModuleCode()).isEqualTo("ANALYSIS");
        assertThat(registered.bizId()).isEqualTo(6001L);
        assertThat(registered.fileCategoryCode()).isEqualTo("VISUAL");
        assertThat(registered.assetTypeCode()).isEqualTo("OVERLAY");
        assertThat(registered.sourceAttachmentId()).isEqualTo(8001L);
        assertThat(registered.bucketName()).isEqualTo("caries-visual");
        assertThat(registered.caseNo()).isEqualTo("CASE-2024-0001");
        assertThat(registered.taskNo()).isEqualTo("TASK1");
        assertThat(registered.modelVersion()).isEqualTo("caries-v2");
        assertThat(registered.relatedImageId()).isEqualTo(7001L);
        assertThat(registered.toothCode()).isEqualTo("16");
        assertThat(registered.orgId()).isEqualTo(100001L);
        assertThat(registered.originalName()).isEqualTo("tmp-abc.png");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisVisualAssetCreateModel>> visualAssetCaptor = ArgumentCaptor.forClass(List.class);
        verify(anaVisualAssetRepository).replaceByTaskId(eq(6001L), visualAssetCaptor.capture());
        List<AnalysisVisualAssetCreateModel> persisted = visualAssetCaptor.getValue();
        assertThat(persisted).hasSize(1);
        AnalysisVisualAssetCreateModel asset = persisted.get(0);
        assertThat(asset.attachmentId()).isEqualTo(9001L);
        assertThat(asset.assetTypeCode()).isEqualTo("OVERLAY");
        assertThat(asset.relatedImageId()).isEqualTo(7001L);
        assertThat(asset.sourceAttachmentId()).isEqualTo(8001L);
        assertThat(asset.toothCode()).isEqualTo("16");
        assertThat(asset.modelVersion()).isEqualTo("caries-v2");
    }

    @Test
    void handleSuccessCallbackWithoutAttachmentIdOrMetadataShouldReject() {
        AnalysisCallbackAppService appService = createServiceWithAttachmentRegistration();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE-2024-0001", 2001L, 100001L, "AI_PROCESSING")));

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "summary":{"overallHighestSeverity":"C1"},
                  "visualAssets":[{"assetTypeCode":"OVERLAY"}]
                }
                """;
        assertThatThrownBy(() -> appService.handleResultCallback(body, "1710000000", "sig"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("attachmentId or bucketName/objectKey");
        verify(attachmentAppService, never()).registerExternalObject(any());
    }

    @Test
    void handleSuccessCallbackWithMismatchedRelatedImageShouldReject() {
        AnalysisCallbackAppService appService = createServiceWithAttachmentRegistration();
        when(anaTaskRecordRepository.findByTaskNo("TASK1")).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(false);
        when(analysisCommandRepository.findImage(7001L)).thenReturn(Optional.of(
                new AnalysisImageModel(7001L, 9999L, 8001L, "PERIAPICAL", "PASS", "caries-image", "other/img.png")));

        String body = """
                {
                  "taskNo":"TASK1",
                  "taskStatusCode":"SUCCESS",
                  "summary":{"overallHighestSeverity":"C1"},
                  "visualAssets":[{
                    "assetTypeCode":"OVERLAY",
                    "attachmentId":4101,
                    "relatedImageId":7001
                  }]
                }
                """;
        assertThatThrownBy(() -> appService.handleResultCallback(body, "1710000000", "sig"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Related image does not belong to analysis case");
        verify(anaVisualAssetRepository, never()).replaceByTaskId(any(), any());
    }
}
