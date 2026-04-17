package com.cariesguard.integration.support;

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
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.ObjectStoreCommand;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.service.ObjectStorageService;
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
import com.cariesguard.followup.app.FollowupTaskAppService;
import com.cariesguard.followup.app.FollowupTriggerService;
import com.cariesguard.followup.domain.model.FollowupCaseModel;
import com.cariesguard.followup.domain.model.FupPlanCreateModel;
import com.cariesguard.followup.domain.model.FupPlanModel;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.domain.model.MsgNotifyCreateModel;
import com.cariesguard.followup.domain.repository.FollowupCaseRepository;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.domain.repository.MsgNotifyRepository;
import com.cariesguard.followup.domain.service.FollowupDomainService;
import com.cariesguard.report.app.ReportAppService;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportAttachmentCreateModel;
import com.cariesguard.report.domain.model.ReportAttachmentModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.domain.model.ReportGenerateModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportTemplateModel;
import com.cariesguard.report.domain.repository.ReportExportLogRepository;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.domain.repository.ReportTemplateRepository;
import com.cariesguard.report.domain.service.ReportDomainService;
import com.cariesguard.report.domain.service.ReportTemplateDomainService;
import com.cariesguard.report.infrastructure.service.ReportPdfService;
import com.cariesguard.report.infrastructure.service.ReportRenderService;
import com.cariesguard.report.infrastructure.service.ReportTemplateResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

public final class AnalysisReportE2EFixture {

    public final SharedState state;
    public final AnalysisTaskAppService analysisTaskAppService;
    public final AnalysisCallbackAppService analysisCallbackAppService;
    public final ReportAppService reportAppService;
    public final InMemoryAnaTaskRecordRepository taskRecordRepository;
    public final InMemoryAnaResultSummaryRepository summaryRepository;
    public final InMemoryAnaVisualAssetRepository visualAssetRepository;
    public final InMemoryMedRiskAssessmentRecordRepository riskRepository;
    public final InMemoryReportRecordRepository reportRecordRepository;
    public final InMemoryReportExportLogRepository reportExportLogRepository;
    public final RecordingAnalysisTaskEventPublisher eventPublisher;
    public final AnalysisProperties analysisProperties;
    public final InMemoryFupPlanRepository fupPlanRepository;
    public final InMemoryFupTaskRepository fupTaskRepository;
    public final InMemoryMsgNotifyRepository msgNotifyRepository;
    public final FollowupTaskAppService followupTaskAppService;
    public final FollowupTriggerService followupTriggerService;

    private AnalysisReportE2EFixture(SharedState state,
                                     AnalysisTaskAppService analysisTaskAppService,
                                     AnalysisCallbackAppService analysisCallbackAppService,
                                     ReportAppService reportAppService,
                                     InMemoryAnaTaskRecordRepository taskRecordRepository,
                                     InMemoryAnaResultSummaryRepository summaryRepository,
                                     InMemoryAnaVisualAssetRepository visualAssetRepository,
                                     InMemoryMedRiskAssessmentRecordRepository riskRepository,
                                     InMemoryReportRecordRepository reportRecordRepository,
                                     InMemoryReportExportLogRepository reportExportLogRepository,
                                     RecordingAnalysisTaskEventPublisher eventPublisher,
                                     AnalysisProperties analysisProperties,
                                     InMemoryFupPlanRepository fupPlanRepository,
                                     InMemoryFupTaskRepository fupTaskRepository,
                                     InMemoryMsgNotifyRepository msgNotifyRepository,
                                     FollowupTaskAppService followupTaskAppService,
                                     FollowupTriggerService followupTriggerService) {
        this.state = state;
        this.analysisTaskAppService = analysisTaskAppService;
        this.analysisCallbackAppService = analysisCallbackAppService;
        this.reportAppService = reportAppService;
        this.taskRecordRepository = taskRecordRepository;
        this.summaryRepository = summaryRepository;
        this.visualAssetRepository = visualAssetRepository;
        this.riskRepository = riskRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.reportExportLogRepository = reportExportLogRepository;
        this.eventPublisher = eventPublisher;
        this.analysisProperties = analysisProperties;
        this.fupPlanRepository = fupPlanRepository;
        this.fupTaskRepository = fupTaskRepository;
        this.msgNotifyRepository = msgNotifyRepository;
        this.followupTaskAppService = followupTaskAppService;
        this.followupTriggerService = followupTriggerService;
    }

    public static AnalysisReportE2EFixture createDefault() {
        SharedState shared = new SharedState();
        shared.caseId = 3001L;
        shared.caseNo = "CASE202604130001";
        shared.patientId = 2001L;
        shared.orgId = 100001L;
        shared.caseStatusCode = "QC_PENDING";
        shared.patientAge = 12;
        shared.genderCode = "MALE";
        shared.images.put(5001L, new AnalysisImageModel(
                5001L, shared.caseId, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/case3001/image01.jpg"));

        InMemoryAnalysisCommandRepository analysisCommandRepository = new InMemoryAnalysisCommandRepository(shared);
        InMemoryAnaTaskRecordRepository taskRecordRepository = new InMemoryAnaTaskRecordRepository();
        InMemoryAnaResultSummaryRepository summaryRepository = new InMemoryAnaResultSummaryRepository(shared);
        InMemoryAnaVisualAssetRepository visualAssetRepository = new InMemoryAnaVisualAssetRepository();
        InMemoryMedRiskAssessmentRecordRepository riskRepository = new InMemoryMedRiskAssessmentRecordRepository();
        InMemoryVisitCaseCommandRepository visitCaseCommandRepository = new InMemoryVisitCaseCommandRepository(shared);
        RecordingAnalysisTaskEventPublisher eventPublisher = new RecordingAnalysisTaskEventPublisher();
        InMemoryReportTemplateRepository reportTemplateRepository = new InMemoryReportTemplateRepository();
        InMemoryReportRecordRepository reportRecordRepository = new InMemoryReportRecordRepository();
        InMemoryReportExportLogRepository reportExportLogRepository = new InMemoryReportExportLogRepository();
        InMemoryReportSourceQueryRepository reportSourceQueryRepository = new InMemoryReportSourceQueryRepository(
                shared, summaryRepository, riskRepository);
        InMemoryObjectStorageService objectStorageService = new InMemoryObjectStorageService();

        reportTemplateRepository.create(new ReportTemplateModel(
                9101L,
                "TPL_DOCTOR_DEFAULT",
                "Doctor Default",
                "DOCTOR",
                "Report No: {{reportNo}}\nCase No: {{caseNo}}\nRisk: {{riskLevelCode}}",
                1,
                shared.orgId,
                "ACTIVE",
                null,
                100001L,
                LocalDateTime.now(),
                LocalDateTime.now()));

        CaseCommandAppService caseCommandAppService = new CaseCommandAppService(
                visitCaseCommandRepository, new CaseStatusMachine());

        AnalysisProperties analysisProperties = new AnalysisProperties();
        analysisProperties.setDefaultModelVersion("caries-v1");
        analysisProperties.setCallbackSecret("p4-analysis-report-e2e-secret");
        analysisProperties.setCallbackAllowedClockSkewSeconds(300);

        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisTaskAppService analysisTaskAppService = new AnalysisTaskAppService(
                analysisCommandRepository,
                taskRecordRepository,
                eventPublisher,
                new AnalysisTaskDomainService(),
                new AnalysisIdempotencyDomainService(taskRecordRepository),
                caseCommandAppService,
                analysisProperties,
                objectMapper);
        AnalysisCallbackAppService analysisCallbackAppService = new AnalysisCallbackAppService(
                new AiCallbackSignatureVerifier(analysisProperties),
                taskRecordRepository,
                summaryRepository,
                visualAssetRepository,
                riskRepository,
                new AnalysisIdempotencyDomainService(taskRecordRepository),
                new AnalysisCallbackDomainService(),
                eventPublisher,
                caseCommandAppService,
                null,
                null,
                objectMapper);
        InMemoryFupPlanRepository fupPlanRepository = new InMemoryFupPlanRepository();
        InMemoryFupTaskRepository fupTaskRepository = new InMemoryFupTaskRepository();
        InMemoryMsgNotifyRepository msgNotifyRepository = new InMemoryMsgNotifyRepository();
        FollowupDomainService followupDomainService = new FollowupDomainService();

        FollowupTriggerService followupTriggerService = new FollowupTriggerService(
                fupPlanRepository,
                fupTaskRepository,
                msgNotifyRepository,
                followupDomainService,
                caseCommandAppService);
        FollowupTaskAppService followupTaskAppService = new FollowupTaskAppService(
                fupTaskRepository,
                fupPlanRepository,
                msgNotifyRepository,
                followupDomainService);

        ReportAppService reportAppService = new ReportAppService(
                reportSourceQueryRepository,
                reportRecordRepository,
                reportExportLogRepository,
                new ReportDomainService(),
                new ReportTemplateResolver(reportTemplateRepository, new ReportTemplateDomainService()),
                new ReportRenderService(),
                new ReportPdfService(),
                objectStorageService,
                caseCommandAppService,
                followupTriggerService);

        return new AnalysisReportE2EFixture(
                shared,
                analysisTaskAppService,
                analysisCallbackAppService,
                reportAppService,
                taskRecordRepository,
                summaryRepository,
                visualAssetRepository,
                riskRepository,
                reportRecordRepository,
                reportExportLogRepository,
                eventPublisher,
                analysisProperties,
                fupPlanRepository,
                fupTaskRepository,
                msgNotifyRepository,
                followupTaskAppService,
                followupTriggerService);
    }

    public void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }

    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    public String currentEpochSecond() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    public String signCallback(String rawBody, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(analysisProperties.getCallbackSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Sign callback failed", exception);
        }
    }

    public String buildSuccessCallbackBody(String taskNo) {
        return """
                {
                  "taskNo":"%s",
                  "taskStatusCode":"SUCCESS",
                  "modelVersion":"caries-v1",
                  "summary":{"overallHighestSeverity":"C2","uncertaintyScore":0.18,"reviewSuggestedFlag":"1"},
                  "rawResultJson":{"overall_highest_severity":"C2","uncertainty_score":0.18,"review_suggested_flag":"1"},
                  "visualAssets":[{"assetTypeCode":"OVERLAY","attachmentId":4101}],
                  "riskAssessment":{"overallRiskLevelCode":"HIGH","assessmentReportJson":{"score":88},"recommendedCycleDays":30}
                }
                """.formatted(taskNo);
    }

    public static final class SharedState {
        public Long caseId;
        public String caseNo;
        public Long patientId;
        public Long orgId;
        public String caseStatusCode;
        public Integer patientAge;
        public String genderCode;
        public String reportReadyFlag = "0";
        public String followupRequiredFlag = "0";
        public final Map<Long, AnalysisImageModel> images = new HashMap<>();
        public final List<CaseStatusLogCreateModel> caseStatusLogs = new ArrayList<>();
        public final Map<Long, AnalysisResultSummaryModel> summariesByTaskId = new HashMap<>();
    }

    public static final class InMemoryAnalysisCommandRepository implements AnalysisCommandRepository {
        private final SharedState shared;

        public InMemoryAnalysisCommandRepository(SharedState shared) {
            this.shared = shared;
        }

        @Override
        public Optional<AnalysisCaseModel> findCase(Long caseId) {
            if (!Objects.equals(shared.caseId, caseId)) {
                return Optional.empty();
            }
            return Optional.of(new AnalysisCaseModel(
                    shared.caseId,
                    shared.caseNo,
                    shared.patientId,
                    shared.orgId,
                    shared.caseStatusCode));
        }

        @Override
        public Optional<AnalysisPatientModel> findPatient(Long patientId) {
            if (!Objects.equals(shared.patientId, patientId)) {
                return Optional.empty();
            }
            return Optional.of(new AnalysisPatientModel(shared.patientId, shared.patientAge, shared.genderCode));
        }

        @Override
        public List<AnalysisImageModel> listCaseImages(Long caseId) {
            if (!Objects.equals(shared.caseId, caseId)) {
                return List.of();
            }
            return shared.images.values().stream()
                    .filter(image -> Objects.equals(image.caseId(), caseId))
                    .sorted(Comparator.comparing(AnalysisImageModel::imageId))
                    .toList();
        }

        @Override
        public Optional<AnalysisImageModel> findImage(Long imageId) {
            return Optional.ofNullable(shared.images.get(imageId));
        }
    }

    public static final class InMemoryVisitCaseCommandRepository implements VisitCaseCommandRepository {
        private final SharedState shared;

        public InMemoryVisitCaseCommandRepository(SharedState shared) {
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
            if (!Objects.equals(shared.caseId, caseId)) {
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
                    .anyMatch(image -> Objects.equals(image.caseId(), caseId) && "PASS".equals(image.qualityStatusCode()));
        }

        @Override
        public boolean hasAiSummary(Long caseId) {
            return shared.summariesByTaskId.values().stream().anyMatch(summary -> Objects.equals(summary.caseId(), caseId));
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

    public static final class InMemoryAnaTaskRecordRepository implements AnaTaskRecordRepository {
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
                    .anyMatch(task -> Objects.equals(task.caseId, caseId)
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
                    .filter(task -> caseId == null || Objects.equals(task.caseId, caseId))
                    .filter(task -> taskStatusCode == null || taskStatusCode.isBlank() || Objects.equals(task.taskStatusCode, taskStatusCode.trim()))
                    .filter(task -> orgId == null || Objects.equals(task.orgId, orgId))
                    .sorted(Comparator.comparing((TaskState item) -> item.createdAt).reversed())
                    .skip(Math.max(offset, 0))
                    .limit(Math.max(limit, 1))
                    .map(TaskState::toView)
                    .toList();
        }

        @Override
        public boolean existsByRetryFromTaskId(Long originalTaskId) {
            return byId.values().stream().anyMatch(task -> Objects.equals(originalTaskId, task.retryFromTaskId));
        }

        public Optional<String> findStatusByTaskNo(String taskNo) {
            return Optional.ofNullable(byTaskNo.get(taskNo)).map(item -> item.taskStatusCode);
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

    public static final class InMemoryAnaResultSummaryRepository implements AnaResultSummaryRepository {
        private final SharedState shared;

        public InMemoryAnaResultSummaryRepository(SharedState shared) {
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
                    .filter(item -> Objects.equals(item.caseId(), caseId))
                    .max(Comparator.comparing(AnalysisResultSummaryModel::summaryId));
        }
    }

    public static final class InMemoryAnaVisualAssetRepository implements AnaVisualAssetRepository {
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

    public static final class InMemoryMedRiskAssessmentRecordRepository implements MedRiskAssessmentRecordRepository {
        private final List<RiskAssessmentCreateModel> records = new ArrayList<>();

        @Override
        public void save(RiskAssessmentCreateModel model) {
            records.add(model);
        }

        public List<RiskAssessmentCreateModel> records() {
            return records;
        }

        public Optional<RiskAssessmentCreateModel> findLatestByCaseId(Long caseId) {
            return records.stream()
                    .filter(item -> Objects.equals(item.caseId(), caseId))
                    .max(Comparator.comparing(RiskAssessmentCreateModel::assessedAt));
        }
    }

    public static final class RecordingAnalysisTaskEventPublisher implements AnalysisTaskEventPublisher {
        public final List<AnalysisRequestedEvent> requestedEvents = new ArrayList<>();
        public final List<AnalysisCompletedEvent> completedEvents = new ArrayList<>();
        public final List<AnalysisFailedEvent> failedEvents = new ArrayList<>();

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

    public static final class InMemoryReportTemplateRepository implements ReportTemplateRepository {
        private final Map<Long, ReportTemplateModel> byId = new HashMap<>();

        @Override
        public Optional<ReportTemplateModel> findById(Long templateId) {
            return Optional.ofNullable(byId.get(templateId));
        }

        @Override
        public Optional<ReportTemplateModel> findLatestActive(Long orgId, String reportTypeCode) {
            return byId.values().stream()
                    .filter(item -> Objects.equals(item.orgId(), orgId))
                    .filter(item -> Objects.equals(item.reportTypeCode(), reportTypeCode))
                    .filter(item -> "ACTIVE".equals(item.status()))
                    .max(Comparator.comparing(ReportTemplateModel::versionNo));
        }

        @Override
        public List<ReportTemplateModel> listByOrgAndType(Long orgId, String reportTypeCode) {
            return byId.values().stream()
                    .filter(item -> Objects.equals(item.orgId(), orgId))
                    .filter(item -> reportTypeCode == null || Objects.equals(item.reportTypeCode(), reportTypeCode))
                    .sorted(Comparator.comparing(ReportTemplateModel::versionNo).reversed())
                    .toList();
        }

        @Override
        public void create(ReportTemplateModel model) {
            byId.put(model.templateId(), model);
        }

        @Override
        public void update(ReportTemplateModel model) {
            ReportTemplateModel current = byId.get(model.templateId());
            if (current == null) {
                return;
            }
            byId.put(model.templateId(), new ReportTemplateModel(
                    current.templateId(),
                    current.templateCode(),
                    StringUtils.hasText(model.templateName()) ? model.templateName() : current.templateName(),
                    current.reportTypeCode(),
                    StringUtils.hasText(model.templateContent()) ? model.templateContent() : current.templateContent(),
                    current.versionNo() + 1,
                    current.orgId(),
                    StringUtils.hasText(model.status()) ? model.status() : current.status(),
                    model.remark(),
                    model.operatorUserId(),
                    current.createdAt(),
                    LocalDateTime.now()));
        }
    }

    public static final class InMemoryReportRecordRepository implements ReportRecordRepository {
        private final Map<Long, ReportRecordModel> reportsById = new HashMap<>();
        private final Map<Long, ReportAttachmentModel> attachmentsById = new HashMap<>();

        @Override
        public int nextVersionNo(Long caseId, String reportTypeCode) {
            return reportsById.values().stream()
                    .filter(item -> Objects.equals(item.caseId(), caseId))
                    .filter(item -> Objects.equals(item.reportTypeCode(), reportTypeCode))
                    .map(ReportRecordModel::versionNo)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
        }

        @Override
        public void create(ReportGenerateModel model) {
            reportsById.put(model.reportId(), new ReportRecordModel(
                    model.reportId(),
                    model.reportNo(),
                    model.caseId(),
                    model.patientId(),
                    null,
                    model.reportTypeCode(),
                    model.reportStatusCode(),
                    model.versionNo(),
                    model.summaryText(),
                    model.generatedAt(),
                    null,
                    model.orgId(),
                    LocalDateTime.now()));
        }

        @Override
        public void updateArchiveInfo(Long reportId,
                                      Long attachmentId,
                                      String reportStatusCode,
                                      LocalDateTime generatedAt,
                                      Long operatorUserId) {
            ReportRecordModel current = reportsById.get(reportId);
            if (current == null) {
                return;
            }
            reportsById.put(reportId, new ReportRecordModel(
                    current.reportId(),
                    current.reportNo(),
                    current.caseId(),
                    current.patientId(),
                    attachmentId,
                    current.reportTypeCode(),
                    reportStatusCode,
                    current.versionNo(),
                    current.summaryText(),
                    generatedAt,
                    current.signedAt(),
                    current.orgId(),
                    current.createdAt()));
        }

        @Override
        public Optional<ReportRecordModel> findById(Long reportId) {
            return Optional.ofNullable(reportsById.get(reportId));
        }

        @Override
        public List<ReportRecordModel> listByCaseId(Long caseId) {
            return reportsById.values().stream()
                    .filter(item -> Objects.equals(item.caseId(), caseId))
                    .sorted(Comparator.comparing(ReportRecordModel::versionNo).reversed())
                    .toList();
        }

        @Override
        public void createAttachment(ReportAttachmentCreateModel model) {
            attachmentsById.put(model.attachmentId(), new ReportAttachmentModel(
                    model.attachmentId(),
                    model.bucketName(),
                    model.objectKey(),
                    model.originalName(),
                    model.contentType(),
                    model.orgId(),
                    model.status()));
        }

        @Override
        public Optional<ReportAttachmentModel> findAttachment(Long attachmentId) {
            return Optional.ofNullable(attachmentsById.get(attachmentId));
        }
    }

    public static final class InMemoryReportSourceQueryRepository implements ReportSourceQueryRepository {
        private final SharedState shared;
        private final InMemoryAnaResultSummaryRepository summaryRepository;
        private final InMemoryMedRiskAssessmentRecordRepository riskRepository;

        public InMemoryReportSourceQueryRepository(SharedState shared,
                                                   InMemoryAnaResultSummaryRepository summaryRepository,
                                                   InMemoryMedRiskAssessmentRecordRepository riskRepository) {
            this.shared = shared;
            this.summaryRepository = summaryRepository;
            this.riskRepository = riskRepository;
        }

        @Override
        public Optional<ReportCaseModel> findCase(Long caseId) {
            if (!Objects.equals(shared.caseId, caseId)) {
                return Optional.empty();
            }
            return Optional.of(new ReportCaseModel(
                    shared.caseId,
                    shared.caseNo,
                    shared.patientId,
                    shared.caseStatusCode,
                    shared.orgId));
        }

        @Override
        public List<ReportImageModel> listCaseImages(Long caseId) {
            if (!Objects.equals(shared.caseId, caseId)) {
                return List.of();
            }
            return shared.images.values().stream()
                    .map(item -> new ReportImageModel(
                            item.imageId(),
                            item.attachmentId(),
                            item.imageTypeCode(),
                            item.qualityStatusCode(),
                            "1",
                            item.bucketName(),
                            item.objectKey(),
                            "image-" + item.imageId() + ".jpg"))
                    .toList();
        }

        @Override
        public Optional<ReportAnalysisSummaryModel> findLatestSummary(Long caseId) {
            return summaryRepository.findLatestByCaseId(caseId)
                    .map(item -> new ReportAnalysisSummaryModel(
                            item.summaryId(),
                            item.rawResultJson(),
                            item.overallHighestSeverity(),
                            item.uncertaintyScore(),
                            item.reviewSuggestedFlag()));
        }

        @Override
        public Optional<ReportRiskAssessmentModel> findLatestRiskAssessment(Long caseId) {
            return riskRepository.findLatestByCaseId(caseId)
                    .map(item -> new ReportRiskAssessmentModel(
                            item.recordId(),
                            item.overallRiskLevelCode(),
                            item.assessmentReportJson(),
                            item.recommendedCycleDays(),
                            item.assessedAt()));
        }

        @Override
        public Optional<ReportCorrectionModel> findLatestCorrection(Long caseId) {
            return Optional.empty();
        }
    }

    public static final class InMemoryReportExportLogRepository implements ReportExportLogRepository {
        private final List<ReportExportLogModel> logs = new ArrayList<>();

        @Override
        public void create(ReportExportLogModel model) {
            logs.add(model);
        }

        public List<ReportExportLogModel> logs() {
            return logs;
        }
    }

    public static final class InMemoryFupPlanRepository implements FupPlanRepository {
        private final Map<Long, FupPlanModel> byId = new HashMap<>();

        @Override
        public void create(FupPlanCreateModel model) {
            byId.put(model.planId(), new FupPlanModel(
                    model.planId(), model.planNo(), model.caseId(), model.patientId(),
                    model.planTypeCode(), model.planStatusCode(), model.nextFollowupDate(),
                    model.intervalDays(), model.ownerUserId(), model.triggerSourceCode(),
                    model.triggerRefId(), model.orgId(), model.remark(), java.time.LocalDateTime.now()));
        }

        @Override
        public Optional<FupPlanModel> findById(Long planId) {
            return Optional.ofNullable(byId.get(planId));
        }

        @Override
        public List<FupPlanModel> listByCaseId(Long caseId) {
            return byId.values().stream()
                    .filter(p -> Objects.equals(p.caseId(), caseId))
                    .toList();
        }

        @Override
        public boolean existsActivePlan(Long caseId, String triggerSourceCode, Long triggerRefId) {
            return byId.values().stream()
                    .anyMatch(p -> Objects.equals(p.caseId(), caseId)
                            && Objects.equals(p.triggerSourceCode(), triggerSourceCode)
                            && Objects.equals(p.triggerRefId(), triggerRefId)
                            && List.of("PLANNED", "ACTIVE").contains(p.planStatusCode()));
        }

        @Override
        public void updateStatus(Long planId, String planStatusCode, Long operatorUserId) {
            FupPlanModel current = byId.get(planId);
            if (current != null) {
                byId.put(planId, new FupPlanModel(
                        current.planId(), current.planNo(), current.caseId(), current.patientId(),
                        current.planTypeCode(), planStatusCode, current.nextFollowupDate(),
                        current.intervalDays(), current.ownerUserId(), current.triggerSourceCode(),
                        current.triggerRefId(), current.orgId(), current.remark(), current.createdAt()));
            }
        }

        public List<FupPlanModel> all() {
            return List.copyOf(byId.values());
        }
    }

    public static final class InMemoryFupTaskRepository implements FupTaskRepository {
        private final Map<Long, FupTaskModel> byId = new HashMap<>();

        @Override
        public void create(FupTaskCreateModel model) {
            byId.put(model.taskId(), new FupTaskModel(
                    model.taskId(), model.taskNo(), model.planId(), model.caseId(), model.patientId(),
                    model.taskTypeCode(), model.taskStatusCode(), model.assignedToUserId(),
                    model.dueDate(), null, null, model.orgId(), model.remark(), java.time.LocalDateTime.now()));
        }

        @Override
        public Optional<FupTaskModel> findById(Long taskId) {
            return Optional.ofNullable(byId.get(taskId));
        }

        @Override
        public List<FupTaskModel> listByPlanId(Long planId) {
            return byId.values().stream().filter(t -> Objects.equals(t.planId(), planId)).toList();
        }

        @Override
        public List<FupTaskModel> listByCaseId(Long caseId) {
            return byId.values().stream().filter(t -> Objects.equals(t.caseId(), caseId)).toList();
        }

        @Override
        public void updateStatus(Long taskId, String taskStatusCode,
                                 java.time.LocalDateTime completedAt, Long operatorUserId) {
            FupTaskModel current = byId.get(taskId);
            if (current != null) {
                byId.put(taskId, new FupTaskModel(
                        current.taskId(), current.taskNo(), current.planId(), current.caseId(),
                        current.patientId(), current.taskTypeCode(), taskStatusCode,
                        current.assignedToUserId(), current.dueDate(), current.startedAt(),
                        completedAt, current.orgId(), current.remark(), current.createdAt()));
            }
        }

        @Override
        public void assignTask(Long taskId, Long assignedToUserId, Long operatorUserId) {
            FupTaskModel current = byId.get(taskId);
            if (current != null) {
                byId.put(taskId, new FupTaskModel(
                        current.taskId(), current.taskNo(), current.planId(), current.caseId(),
                        current.patientId(), current.taskTypeCode(), current.taskStatusCode(),
                        assignedToUserId, current.dueDate(), current.startedAt(),
                        current.completedAt(), current.orgId(), current.remark(), current.createdAt()));
            }
        }

        @Override
        public boolean allTasksDoneOrCancelled(Long planId) {
            return byId.values().stream()
                    .filter(t -> Objects.equals(t.planId(), planId))
                    .allMatch(t -> List.of("DONE", "CANCELLED").contains(t.taskStatusCode()));
        }

        public List<FupTaskModel> all() {
            return List.copyOf(byId.values());
        }
    }

    public static final class InMemoryMsgNotifyRepository implements MsgNotifyRepository {
        private final List<MsgNotifyCreateModel> records = new ArrayList<>();

        @Override
        public void create(MsgNotifyCreateModel model) {
            records.add(model);
        }

        public List<MsgNotifyCreateModel> all() {
            return List.copyOf(records);
        }
    }

    public static final class InMemoryFollowupCaseRepository implements FollowupCaseRepository {
        private final SharedState shared;

        public InMemoryFollowupCaseRepository(SharedState shared) {
            this.shared = shared;
        }

        @Override
        public Optional<FollowupCaseModel> findCase(Long caseId) {
            if (!Objects.equals(shared.caseId, caseId)) {
                return Optional.empty();
            }
            return Optional.of(new FollowupCaseModel(
                    shared.caseId, shared.patientId, shared.caseStatusCode, shared.orgId));
        }
    }

    public static final class InMemoryObjectStorageService implements ObjectStorageService {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public StoredObject store(ObjectStoreCommand command) throws IOException {
            byte[] bytes = readAll(command.inputStream());
            String objectKey = command.objectKindCode() + "/" + Instant.now().toEpochMilli() + "/" + command.originalFileName();
            objects.put(objectKey, bytes);
            String bucketName = switch (command.bucketCode()) {
                case "REPORT" -> "caries-report";
                case "EXPORT" -> "caries-export";
                case "VISUAL" -> "caries-visual";
                default -> "caries-image";
            };
            return new StoredObject(bucketName, objectKey, command.originalFileName(), command.contentType(), command.fileSizeBytes(), command.md5(), "MINIO");
        }

        @Override
        public StoredObject store(String originalFileName,
                                  String contentType,
                                  InputStream inputStream,
                                  long fileSizeBytes,
                                  String md5) throws IOException {
            return store(ObjectStoreCommand.rawImage("IMAGE", 0L, "UNBOUND", "RAW_IMAGE", 0L, originalFileName, contentType, inputStream, fileSizeBytes, md5));
        }

        @Override
        public StoredObjectResource load(String bucketName,
                                         String objectKey,
                                         String originalFileName,
                                         String contentType) {
            byte[] bytes = objects.get(objectKey);
            if (bytes == null) {
                throw new IllegalStateException("Object does not exist: " + objectKey);
            }
            return new StoredObjectResource(
                    new ByteArrayResource(bytes),
                    StringUtils.hasText(contentType) ? contentType : "application/octet-stream",
                    StringUtils.hasText(originalFileName) ? originalFileName : objectKey,
                    bytes.length);
        }

        @Override
        public void delete(String bucketName, String objectKey) {
            objects.remove(objectKey);
        }

        @Override
        public String presignGetObject(String bucketName, String objectKey) {
            return "http://minio.test/" + bucketName + "/" + objectKey + "?signature=test";
        }

        @Override
        public long defaultPresignExpireSeconds() {
            return 900L;
        }

        @Override
        public String proxyAccessSecret() {
            return "integration-test-image-secret";
        }

        private byte[] readAll(InputStream inputStream) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
