package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.app.AnalysisCallbackAppService;
import com.cariesguard.analysis.app.AnalysisTaskAppService;
import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskStatusUpdateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetModel;
import com.cariesguard.analysis.domain.model.RiskAssessmentCreateModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.repository.MedRiskAssessmentRecordRepository;
import com.cariesguard.analysis.domain.service.AnalysisCallbackDomainService;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.infrastructure.client.AiCallbackSignatureVerifier;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.app.CaseStatusMachine;
import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.CaseStatusUpdateModel;
import com.cariesguard.patient.domain.model.CaseToothRecordCreateModel;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AnalysisWorkflowE2ETests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRunCaseAnalysisMainlineAcrossPatientAndAnalysisModules() {
        SharedState shared = new SharedState();
        shared.caseId = 3001L;
        shared.caseNo = "CASE202604130001";
        shared.caseStatusCode = "QC_PENDING";
        shared.patientId = 2001L;
        shared.orgId = 100001L;
        shared.patientAge = 12;
        shared.genderCode = "MALE";
        shared.images.put(5001L, new AnalysisImageModel(
                5001L, shared.caseId, 4001L, "PANORAMIC", "PASS",
                "caries-image", "attachments/case3001/image01.jpg"));

        InMemoryAnalysisCommandRepository analysisCommandRepository = new InMemoryAnalysisCommandRepository(shared);
        InMemoryAnaTaskRecordRepository anaTaskRecordRepository = new InMemoryAnaTaskRecordRepository();
        InMemoryAnaResultSummaryRepository anaResultSummaryRepository = new InMemoryAnaResultSummaryRepository(shared);
        InMemoryAnaVisualAssetRepository anaVisualAssetRepository = new InMemoryAnaVisualAssetRepository();
        InMemoryMedRiskAssessmentRecordRepository medRiskAssessmentRecordRepository = new InMemoryMedRiskAssessmentRecordRepository();
        InMemoryVisitCaseCommandRepository visitCaseCommandRepository = new InMemoryVisitCaseCommandRepository(shared);
        RecordingAnalysisTaskEventPublisher eventPublisher = new RecordingAnalysisTaskEventPublisher();

        CaseCommandAppService caseCommandAppService = new CaseCommandAppService(
                visitCaseCommandRepository, new CaseStatusMachine());

        AnalysisProperties analysisProperties = new AnalysisProperties();
        analysisProperties.setDefaultModelVersion("caries-v1");
        analysisProperties.setCallbackSecret("p4-analysis-e2e-secret");
        analysisProperties.setCallbackAllowedClockSkewSeconds(300);

        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisTaskAppService taskAppService = new AnalysisTaskAppService(
                analysisCommandRepository,
                anaTaskRecordRepository,
                eventPublisher,
                new AnalysisTaskDomainService(),
                new AnalysisIdempotencyDomainService(anaTaskRecordRepository),
                caseCommandAppService,
                analysisProperties,
                objectMapper);
        AnalysisCallbackAppService callbackAppService = new AnalysisCallbackAppService(
                new AiCallbackSignatureVerifier(analysisProperties),
                anaTaskRecordRepository,
                anaResultSummaryRepository,
                anaVisualAssetRepository,
                medRiskAssessmentRecordRepository,
                new AnalysisIdempotencyDomainService(anaTaskRecordRepository),
                new AnalysisCallbackDomainService(),
                eventPublisher,
                caseCommandAppService,
                null,
                null,
                objectMapper);

        setCurrentUser(new AuthenticatedUser(
                100001L, shared.orgId, "sys-admin", "hash", "System Admin", true, List.of("SYS_ADMIN")));

        AnalysisTaskVO task = taskAppService.createTask(new CreateAnalysisTaskCommand(
                shared.caseId, shared.patientId, false, "INFERENCE", "e2e-mainline"));

        assertThat(task.taskStatusCode()).isEqualTo("QUEUEING");
        assertThat(shared.caseStatusCode).isEqualTo("ANALYZING");
        assertThat(eventPublisher.requestedEvents).hasSize(1);
        assertThat(shared.caseStatusLogs).hasSize(1);
        assertThat(shared.caseStatusLogs.get(0).toStatusCode()).isEqualTo("ANALYZING");

        String callbackBody = """
                {
                  "taskNo":"%s",
                  "taskStatusCode":"SUCCESS",
                  "modelVersion":"caries-v1",
                  "summary":{"overallHighestSeverity":"C2","uncertaintyScore":0.18,"reviewSuggestedFlag":"1","teethCount":28},
                  "rawResultJson":{"overall_highest_severity":"C2","uncertainty_score":0.18,"review_suggested_flag":"1"},
                  "visualAssets":[{"assetTypeCode":"OVERLAY","attachmentId":4101}],
                  "riskAssessment":{"overallRiskLevelCode":"HIGH","assessmentReportJson":{"score":88},"recommendedCycleDays":30}
                }
                """.formatted(task.taskNo());
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = signLikePython(callbackBody, timestamp, analysisProperties.getCallbackSecret());

        AnalysisCallbackAckVO ack = callbackAppService.handleResultCallback(callbackBody, timestamp, signature);

        assertThat(ack.taskStatusCode()).isEqualTo("SUCCESS");
        assertThat(ack.idempotent()).isFalse();
        assertThat(shared.caseStatusCode).isEqualTo("REVIEW_PENDING");
        assertThat(shared.caseStatusLogs).hasSize(2);
        assertThat(shared.caseStatusLogs.get(1).fromStatusCode()).isEqualTo("ANALYZING");
        assertThat(shared.caseStatusLogs.get(1).toStatusCode()).isEqualTo("REVIEW_PENDING");

        AnalysisTaskViewModel storedTask = anaTaskRecordRepository.findByTaskNo(task.taskNo()).orElseThrow();
        assertThat(storedTask.taskStatusCode()).isEqualTo("SUCCESS");
        assertThat(anaResultSummaryRepository.findByTaskId(task.taskId())).isPresent();
        assertThat(anaVisualAssetRepository.listByTaskId(task.taskId())).hasSize(1);
        assertThat(medRiskAssessmentRecordRepository.savedRecords).hasSize(1);
        assertThat(eventPublisher.completedEvents).hasSize(1);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }

    private String signLikePython(String rawBody, String timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Sign callback failed", exception);
        }
    }

    private static final class SharedState {
        Long caseId;
        String caseNo;
        Long patientId;
        Long orgId;
        String caseStatusCode;
        Integer patientAge;
        String genderCode;
        String reportReadyFlag = "0";
        String followupRequiredFlag = "0";
        final Map<Long, AnalysisImageModel> images = new HashMap<>();
        final List<CaseStatusLogCreateModel> caseStatusLogs = new ArrayList<>();
        final Map<Long, AnalysisResultSummaryModel> summariesByTaskId = new HashMap<>();
    }

    private static final class InMemoryAnalysisCommandRepository implements AnalysisCommandRepository {
        private final SharedState shared;

        private InMemoryAnalysisCommandRepository(SharedState shared) {
            this.shared = shared;
        }

        @Override
        public Optional<AnalysisCaseModel> findCase(Long caseId) {
            if (!shared.caseId.equals(caseId)) {
                return Optional.empty();
            }
            return Optional.of(new AnalysisCaseModel(
                    shared.caseId, shared.caseNo, shared.patientId, shared.orgId, shared.caseStatusCode));
        }

        @Override
        public Optional<AnalysisPatientModel> findPatient(Long patientId) {
            if (!shared.patientId.equals(patientId)) {
                return Optional.empty();
            }
            return Optional.of(new AnalysisPatientModel(shared.patientId, shared.patientAge, shared.genderCode));
        }

        @Override
        public List<AnalysisImageModel> listCaseImages(Long caseId) {
            if (!shared.caseId.equals(caseId)) {
                return List.of();
            }
            return shared.images.values().stream()
                    .filter(image -> image.caseId().equals(caseId))
                    .sorted(Comparator.comparing(AnalysisImageModel::imageId))
                    .toList();
        }

        @Override
        public Optional<AnalysisImageModel> findImage(Long imageId) {
            return Optional.ofNullable(shared.images.get(imageId));
        }
    }

    private static final class InMemoryVisitCaseCommandRepository implements VisitCaseCommandRepository {
        private final SharedState shared;

        private InMemoryVisitCaseCommandRepository(SharedState shared) {
            this.shared = shared;
        }

        @Override
        public Optional<PatientOwnedModel> findPatient(Long patientId) {
            return Optional.empty();
        }

        @Override
        public Optional<VisitOwnedModel> findVisit(Long visitId) {
            return Optional.empty();
        }

        @Override
        public void createVisit(VisitCreateModel model) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createCase(CaseCreateModel model, CaseStatusLogCreateModel statusLog) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CaseManagedModel> findManagedCase(Long caseId) {
            if (!shared.caseId.equals(caseId)) {
                return Optional.empty();
            }
            return Optional.of(new CaseManagedModel(
                    shared.caseId,
                    shared.caseStatusCode,
                    shared.reportReadyFlag,
                    shared.followupRequiredFlag,
                    shared.orgId));
        }

        @Override
        public boolean hasActiveImage(Long caseId) {
            return shared.images.values().stream()
                    .anyMatch(image -> image.caseId().equals(caseId) && "PASS".equals(image.qualityStatusCode()));
        }

        @Override
        public boolean hasAiSummary(Long caseId) {
            return shared.summariesByTaskId.values().stream()
                    .anyMatch(summary -> summary.caseId().equals(caseId));
        }

        @Override
        public void updateCaseStatus(CaseStatusUpdateModel model) {
            shared.caseStatusCode = model.caseStatusCode();
            shared.reportReadyFlag = model.reportReadyFlag();
            shared.followupRequiredFlag = model.followupRequiredFlag();
        }

        @Override
        public void appendCaseStatusLog(CaseStatusLogCreateModel model) {
            shared.caseStatusLogs.add(model);
        }

        @Override
        public void replaceDiagnoses(Long caseId, Long operatorUserId, LocalDateTime diagnosisTime, List<CaseDiagnosisCreateModel> diagnoses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceToothRecords(Long caseId, Long operatorUserId, List<CaseToothRecordCreateModel> toothRecords) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryAnaTaskRecordRepository implements AnaTaskRecordRepository {
        private final Map<Long, TaskState> byId = new HashMap<>();
        private final Map<String, TaskState> byTaskNo = new HashMap<>();

        @Override
        public void save(AnalysisTaskCreateModel model) {
            TaskState state = new TaskState();
            state.taskId = model.taskId();
            state.taskNo = model.taskNo();
            state.caseId = model.caseId();
            state.patientId = model.patientId();
            state.modelVersion = model.modelVersion();
            state.taskTypeCode = model.taskTypeCode();
            state.taskStatusCode = model.taskStatusCode();
            state.errorMessage = null;
            state.createdAt = LocalDateTime.now();
            state.orgId = model.orgId();
            state.retryFromTaskId = model.retryFromTaskId();
            byId.put(state.taskId, state);
            byTaskNo.put(state.taskNo, state);
        }

        @Override
        public Optional<AnalysisTaskViewModel> findById(Long taskId) {
            return Optional.ofNullable(byId.get(taskId)).map(TaskState::toView);
        }

        @Override
        public Optional<AnalysisTaskViewModel> findByTaskNo(String taskNo) {
            return Optional.ofNullable(byTaskNo.get(taskNo)).map(TaskState::toView);
        }

        @Override
        public boolean existsRunningTaskByCaseId(Long caseId) {
            return byId.values().stream()
                    .anyMatch(task -> task.caseId.equals(caseId)
                            && List.of("QUEUEING", "PROCESSING").contains(task.taskStatusCode));
        }

        @Override
        public void updateStatus(AnalysisTaskStatusUpdateModel model) {
            TaskState state = byTaskNo.get(model.taskNo());
            if (state == null) {
                return;
            }
            state.taskStatusCode = model.taskStatusCode();
            state.errorMessage = model.errorMessage();
            state.startedAt = model.startedAt();
            state.completedAt = model.completedAt();
        }

        @Override
        public long count(Long caseId, String taskStatusCode, Long orgId) {
            return pageQuery(caseId, taskStatusCode, orgId, 0, Integer.MAX_VALUE).size();
        }

        @Override
        public List<AnalysisTaskViewModel> pageQuery(Long caseId, String taskStatusCode, Long orgId, int offset, int limit) {
            return byId.values().stream()
                    .filter(task -> caseId == null || task.caseId.equals(caseId))
                    .filter(task -> taskStatusCode == null || taskStatusCode.isBlank() || task.taskStatusCode.equals(taskStatusCode.trim()))
                    .filter(task -> orgId == null || task.orgId.equals(orgId))
                    .sorted(Comparator.comparing((TaskState task) -> task.createdAt).reversed())
                    .skip(Math.max(offset, 0))
                    .limit(Math.max(limit, 1))
                    .map(TaskState::toView)
                    .toList();
        }

        @Override
        public boolean existsByRetryFromTaskId(Long originalTaskId) {
            return byId.values().stream().anyMatch(task -> originalTaskId.equals(task.retryFromTaskId));
        }

        private static final class TaskState {
            private Long taskId;
            private String taskNo;
            private Long caseId;
            private Long patientId;
            private String modelVersion;
            private String taskTypeCode;
            private String taskStatusCode;
            private String errorMessage;
            private LocalDateTime createdAt;
            private LocalDateTime startedAt;
            private LocalDateTime completedAt;
            private Long orgId;
            private Long retryFromTaskId;

            private AnalysisTaskViewModel toView() {
                return new AnalysisTaskViewModel(
                        taskId,
                        taskNo,
                        caseId,
                        patientId,
                        modelVersion,
                        taskTypeCode,
                        taskStatusCode,
                        errorMessage,
                        createdAt,
                        startedAt,
                        completedAt,
                        orgId,
                        retryFromTaskId);
            }
        }
    }

    private static final class InMemoryAnaResultSummaryRepository implements AnaResultSummaryRepository {
        private final SharedState shared;

        private InMemoryAnaResultSummaryRepository(SharedState shared) {
            this.shared = shared;
        }

        @Override
        public void save(AnalysisResultSummaryModel model) {
            shared.summariesByTaskId.put(model.taskId(), model);
        }

        @Override
        public Optional<AnalysisResultSummaryModel> findByTaskId(Long taskId) {
            return Optional.ofNullable(shared.summariesByTaskId.get(taskId));
        }

        @Override
        public Optional<AnalysisResultSummaryModel> findLatestByCaseId(Long caseId) {
            return shared.summariesByTaskId.values().stream()
                    .filter(item -> item.caseId().equals(caseId))
                    .max(Comparator.comparing(AnalysisResultSummaryModel::summaryId));
        }
    }

    private static final class InMemoryAnaVisualAssetRepository implements AnaVisualAssetRepository {
        private final Map<Long, List<AnalysisVisualAssetModel>> byTaskId = new HashMap<>();

        @Override
        public void replaceByTaskId(Long taskId, List<AnalysisVisualAssetCreateModel> models) {
            byTaskId.put(taskId, models.stream()
                    .map(item -> new AnalysisVisualAssetModel(item.assetTypeCode(), item.attachmentId(), null, null))
                    .toList());
        }

        @Override
        public List<AnalysisVisualAssetModel> listByTaskId(Long taskId) {
            return byTaskId.getOrDefault(taskId, List.of());
        }
    }

    private static final class InMemoryMedRiskAssessmentRecordRepository implements MedRiskAssessmentRecordRepository {
        private final List<RiskAssessmentCreateModel> savedRecords = new ArrayList<>();

        @Override
        public void save(RiskAssessmentCreateModel model) {
            savedRecords.add(model);
        }
    }

    private static final class RecordingAnalysisTaskEventPublisher implements AnalysisTaskEventPublisher {
        private final List<AnalysisRequestedEvent> requestedEvents = new ArrayList<>();
        private final List<AnalysisCompletedEvent> completedEvents = new ArrayList<>();
        private final List<AnalysisFailedEvent> failedEvents = new ArrayList<>();

        @Override
        public void publishRequested(AnalysisRequestedEvent event) {
            requestedEvents.add(event);
        }

        @Override
        public void publishCompleted(AnalysisCompletedEvent event) {
            completedEvents.add(event);
        }

        @Override
        public void publishFailed(AnalysisFailedEvent event) {
            failedEvents.add(event);
        }
    }
}
