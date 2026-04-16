package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskStatusUpdateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.model.RiskAssessmentCreateModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.repository.MedRiskAssessmentRecordRepository;
import com.cariesguard.analysis.domain.service.AnalysisCallbackDomainService;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.infrastructure.client.AiCallbackSignatureVerifier;
import com.cariesguard.analysis.interfaces.command.AiAnalysisResultCallbackCommand;
import com.cariesguard.analysis.interfaces.dto.AiVisualAssetDTO;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.domain.model.AttachmentObjectRegistrationModel;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnalysisCallbackAppService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisCallbackAppService.class);

    private final AiCallbackSignatureVerifier aiCallbackSignatureVerifier;
    private final AnaTaskRecordRepository anaTaskRecordRepository;
    private final AnaResultSummaryRepository anaResultSummaryRepository;
    private final AnaVisualAssetRepository anaVisualAssetRepository;
    private final MedRiskAssessmentRecordRepository medRiskAssessmentRecordRepository;
    private final AnalysisIdempotencyDomainService analysisIdempotencyDomainService;
    private final AnalysisCallbackDomainService analysisCallbackDomainService;
    private final AnalysisTaskEventPublisher analysisTaskEventPublisher;
    private final CaseCommandAppService caseCommandAppService;
    private final AttachmentAppService attachmentAppService;
    private final AnalysisCommandRepository analysisCommandRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalysisCallbackAppService(AiCallbackSignatureVerifier aiCallbackSignatureVerifier,
                                      AnaTaskRecordRepository anaTaskRecordRepository,
                                      AnaResultSummaryRepository anaResultSummaryRepository,
                                      AnaVisualAssetRepository anaVisualAssetRepository,
                                      MedRiskAssessmentRecordRepository medRiskAssessmentRecordRepository,
                                      AnalysisIdempotencyDomainService analysisIdempotencyDomainService,
                                      AnalysisCallbackDomainService analysisCallbackDomainService,
                                      AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                      CaseCommandAppService caseCommandAppService,
                                      AttachmentAppService attachmentAppService,
                                      AnalysisCommandRepository analysisCommandRepository,
                                      ObjectMapper objectMapper) {
        this.aiCallbackSignatureVerifier = aiCallbackSignatureVerifier;
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.anaResultSummaryRepository = anaResultSummaryRepository;
        this.anaVisualAssetRepository = anaVisualAssetRepository;
        this.medRiskAssessmentRecordRepository = medRiskAssessmentRecordRepository;
        this.analysisIdempotencyDomainService = analysisIdempotencyDomainService;
        this.analysisCallbackDomainService = analysisCallbackDomainService;
        this.analysisTaskEventPublisher = analysisTaskEventPublisher;
        this.caseCommandAppService = caseCommandAppService;
        this.attachmentAppService = attachmentAppService;
        this.analysisCommandRepository = analysisCommandRepository;
        this.objectMapper = objectMapper;
    }

    public AnalysisCallbackAppService(AiCallbackSignatureVerifier aiCallbackSignatureVerifier,
                                      AnaTaskRecordRepository anaTaskRecordRepository,
                                      AnaResultSummaryRepository anaResultSummaryRepository,
                                      AnaVisualAssetRepository anaVisualAssetRepository,
                                      MedRiskAssessmentRecordRepository medRiskAssessmentRecordRepository,
                                      AnalysisIdempotencyDomainService analysisIdempotencyDomainService,
                                      AnalysisCallbackDomainService analysisCallbackDomainService,
                                      AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                      CaseCommandAppService caseCommandAppService,
                                      ObjectMapper objectMapper) {
        this(aiCallbackSignatureVerifier, anaTaskRecordRepository, anaResultSummaryRepository, anaVisualAssetRepository,
                medRiskAssessmentRecordRepository, analysisIdempotencyDomainService, analysisCallbackDomainService,
                analysisTaskEventPublisher, caseCommandAppService, null, null, objectMapper);
    }

    @Transactional
    public AnalysisCallbackAckVO handleResultCallback(String rawBody, String timestamp, String signature) {
        aiCallbackSignatureVerifier.verify(rawBody, timestamp, signature);
        AiAnalysisResultCallbackCommand callback = readCallback(rawBody);
        String taskNo = analysisCallbackDomainService.normalizeAndValidateTaskNo(callback.taskNo());
        String incomingStatus = analysisCallbackDomainService.normalizeAndValidateTaskStatus(callback.taskStatusCode());
        AnalysisTaskViewModel task = anaTaskRecordRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));

        if (analysisIdempotencyDomainService.isDuplicateTerminalCallback(task, incomingStatus)) {
            return new AnalysisCallbackAckVO(task.taskNo(), task.taskStatusCode(), true);
        }
        analysisIdempotencyDomainService.ensureCallbackAllowed(task, incomingStatus);

        if (analysisIdempotencyDomainService.hasBeenRetried(task.taskId())) {
            log.warn("Late callback on retried task taskNo={} incomingStatus={}, logging only", task.taskNo(), incomingStatus);
            return new AnalysisCallbackAckVO(task.taskNo(), incomingStatus, true);
        }

        return switch (incomingStatus) {
            case "PROCESSING" -> processProcessing(task, callback, rawBody);
            case "SUCCESS" -> processSuccess(task, callback, rawBody);
            case "FAILED" -> processFailure(task, callback, rawBody);
            default -> throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        };
    }

    private AnalysisCallbackAckVO processProcessing(AnalysisTaskViewModel task,
                                                   AiAnalysisResultCallbackCommand callback,
                                                   String rawBody) {
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "PROCESSING",
                rawBody,
                null,
                null,
                defaultStartedAt(callback.startedAt()),
                null,
                trimToNull(callback.traceId()),
                callback.inferenceMillis(),
                callback.modelVersion()));
        return new AnalysisCallbackAckVO(task.taskNo(), "PROCESSING", false);
    }

    private AnalysisCallbackAckVO processSuccess(AnalysisTaskViewModel task,
                                                AiAnalysisResultCallbackCommand callback,
                                                String rawBody) {
        analysisCallbackDomainService.validateSuccessCallbackCompleteness(callback);
        AnalysisCallbackDomainService.SummaryAggregates aggregates =
                analysisCallbackDomainService.extractSummaryAggregates(callback.summary(), callback.rawResultJson(), callback.uncertaintyScore());

        String rawResultJson = resolveRawResultJson(callback);
        String resolvedModelVersion = StringUtils.hasText(callback.modelVersion()) ? callback.modelVersion().trim() : task.modelVersion();
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "SUCCESS",
                rawBody,
                null,
                null,
                defaultStartedAt(callback.startedAt()),
                defaultCompletedAt(callback.completedAt()),
                trimToNull(callback.traceId()),
                callback.inferenceMillis(),
                resolvedModelVersion));
        anaResultSummaryRepository.save(new AnalysisResultSummaryModel(
                IdWorker.getId(),
                task.taskId(),
                task.caseId(),
                rawResultJson,
                aggregates.overallHighestSeverity(),
                aggregates.uncertaintyScore(),
                aggregates.reviewSuggestedFlag(),
                extractLesionCount(callback.rawResultJson()),
                extractAbnormalToothCount(callback.rawResultJson()),
                1,
                task.orgId(),
                0L));
        anaVisualAssetRepository.replaceByTaskId(task.taskId(), buildVisualAssets(task, callback.visualAssets(), resolvedModelVersion));
        if (callback.riskAssessment() != null && StringUtils.hasText(callback.riskAssessment().overallRiskLevelCode())) {
            medRiskAssessmentRecordRepository.save(new RiskAssessmentCreateModel(
                    IdWorker.getId(),
                    task.caseId(),
                    task.patientId(),
                    task.taskId(),
                    callback.riskAssessment().overallRiskLevelCode().trim(),
                    callback.riskAssessment().riskScore(),
                    toJson(callback.riskAssessment().assessmentReportJson()),
                    callback.riskAssessment().recommendedCycleDays(),
                    1,
                    defaultCompletedAt(callback.completedAt()),
                    task.orgId(),
                    0L));
        }

        String targetStatus = analysisCallbackDomainService.resolveTargetCaseStatus("SUCCESS");
        String reasonCode = analysisCallbackDomainService.resolveChangeReasonCode("SUCCESS");
        caseCommandAppService.transitionStatusAsSystem(task.caseId(), task.orgId(), new CaseStatusTransitionCommand(
                targetStatus, reasonCode, "AI callback success: " + task.taskNo()));

        analysisTaskEventPublisher.publishCompleted(new AnalysisCompletedEvent(
                task.taskId(), task.taskNo(), task.caseId(), resolvedModelVersion,
                defaultCompletedAt(callback.completedAt())));

        return new AnalysisCallbackAckVO(task.taskNo(), "SUCCESS", false);
    }

    private AnalysisCallbackAckVO processFailure(AnalysisTaskViewModel task,
                                                AiAnalysisResultCallbackCommand callback,
                                                String rawBody) {
        String errorCode = resolveErrorCode(callback);
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "FAILED",
                rawBody,
                errorCode,
                trimToNull(callback.errorMessage()),
                defaultStartedAt(callback.startedAt()),
                defaultCompletedAt(callback.completedAt()),
                trimToNull(callback.traceId()),
                callback.inferenceMillis(),
                callback.modelVersion()));

        String targetStatus = analysisCallbackDomainService.resolveTargetCaseStatus("FAILED");
        String reasonCode = analysisCallbackDomainService.resolveChangeReasonCode("FAILED");
        caseCommandAppService.transitionStatusAsSystem(task.caseId(), task.orgId(), new CaseStatusTransitionCommand(
                targetStatus, reasonCode,
                defaultRemark(callback.errorMessage(), "AI callback failed: " + task.taskNo())));

        analysisTaskEventPublisher.publishFailed(new AnalysisFailedEvent(
                task.taskId(), task.taskNo(), task.caseId(),
                trimToNull(callback.errorMessage()),
                defaultCompletedAt(callback.completedAt())));

        return new AnalysisCallbackAckVO(task.taskNo(), "FAILED", false);
    }

    private AiAnalysisResultCallbackCommand readCallback(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, AiAnalysisResultCallbackCommand.class);
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "AI callback payload is invalid");
        }
    }

    private String resolveRawResultJson(AiAnalysisResultCallbackCommand callback) {
        if (callback.rawResultJson() != null && !callback.rawResultJson().isNull()) {
            return toJson(callback.rawResultJson());
        }
        if (callback.summary() != null) {
            return toJson(callback.summary());
        }
        return null;
    }

    private List<AnalysisVisualAssetCreateModel> buildVisualAssets(AnalysisTaskViewModel task,
                                                                   List<AiVisualAssetDTO> visualAssets,
                                                                   String modelVersion) {
        if (visualAssets == null || visualAssets.isEmpty()) {
            return List.of();
        }
        String resolvedModelVersion = StringUtils.hasText(modelVersion) ? modelVersion.trim() : task.modelVersion();
        AnalysisCaseModel medicalCase = visualAssets.stream().anyMatch(item -> item.attachmentId() == null)
                ? loadCaseForVisualAssets(task)
                : null;
        List<AnalysisVisualAssetCreateModel> models = new ArrayList<>();
        for (int index = 0; index < visualAssets.size(); index++) {
            AiVisualAssetDTO item = visualAssets.get(index);
            String assetTypeCode = normalizeAssetType(item.assetTypeCode());
            Long sourceAttachmentId = resolveSourceAttachmentId(task, item.relatedImageId());
            Long attachmentId = resolveVisualAttachmentId(task, medicalCase, item, assetTypeCode, resolvedModelVersion, sourceAttachmentId);
            models.add(new AnalysisVisualAssetCreateModel(
                    IdWorker.getId(),
                    task.taskId(),
                    task.caseId(),
                    resolvedModelVersion,
                    assetTypeCode,
                    attachmentId,
                    item.relatedImageId(),
                    sourceAttachmentId,
                    trimToNull(item.toothCode()),
                    index,
                    task.orgId(),
                    0L));
        }
        return models;
    }

    private Long resolveVisualAttachmentId(AnalysisTaskViewModel task,
                                           AnalysisCaseModel medicalCase,
                                           AiVisualAssetDTO item,
                                           String assetTypeCode,
                                           String modelVersion,
                                           Long sourceAttachmentId) {
        if (item.attachmentId() != null) {
            return item.attachmentId();
        }
        if (!StringUtils.hasText(item.bucketName()) || !StringUtils.hasText(item.objectKey())) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "visualAssets item must contain attachmentId or bucketName/objectKey");
        }
        if (attachmentAppService == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR.code(), "Attachment registration service is not available");
        }
        AttachmentUploadVO registered = attachmentAppService.registerExternalObject(new AttachmentObjectRegistrationModel(
                null,
                "ANALYSIS",
                task.taskId(),
                "VISUAL",
                assetTypeCode,
                sourceAttachmentId,
                fileNameFromObjectKey(item.objectKey()),
                item.bucketName(),
                item.objectKey(),
                item.contentType(),
                item.fileSizeBytes(),
                trimToNull(item.md5()),
                "PRIVATE",
                null,
                null,
                null,
                task.orgId(),
                "ACTIVE",
                "AI visual asset callback for task " + task.taskNo(),
                0L,
                medicalCase.caseNo(),
                task.taskNo(),
                modelVersion,
                item.relatedImageId(),
                trimToNull(item.toothCode())));
        return registered.attachmentId();
    }

    private String normalizeAssetType(String assetTypeCode) {
        if (!StringUtils.hasText(assetTypeCode)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "visual asset assetTypeCode is required");
        }
        return assetTypeCode.trim().toUpperCase(Locale.ROOT);
    }

    private AnalysisCaseModel loadCaseForVisualAssets(AnalysisTaskViewModel task) {
        if (analysisCommandRepository == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR.code(), "Analysis case query service is not available");
        }
        return analysisCommandRepository.findCase(task.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
    }

    private Long resolveSourceAttachmentId(AnalysisTaskViewModel task, Long relatedImageId) {
        if (relatedImageId == null) {
            return null;
        }
        if (analysisCommandRepository == null) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR.code(), "Analysis image query service is not available");
        }
        AnalysisImageModel image = analysisCommandRepository.findImage(relatedImageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Related image does not exist"));
        if (!task.caseId().equals(image.caseId())) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Related image does not belong to analysis case");
        }
        return image.attachmentId();
    }

    private Integer extractLesionCount(JsonNode rawResultJson) {
        Integer directValue = intValue(rawResultJson, "lesionCount", "lesion_count");
        if (directValue != null) {
            return directValue;
        }
        JsonNode lesionResults = arrayValue(rawResultJson, "lesionResults", "lesion_results");
        return lesionResults == null ? null : lesionResults.size();
    }

    private Integer extractAbnormalToothCount(JsonNode rawResultJson) {
        Integer directValue = intValue(rawResultJson, "abnormalToothCount", "abnormal_tooth_count");
        if (directValue != null) {
            return directValue;
        }
        JsonNode lesionResults = arrayValue(rawResultJson, "lesionResults", "lesion_results");
        if (lesionResults == null) {
            return null;
        }
        Set<String> toothCodes = new LinkedHashSet<>();
        for (JsonNode lesion : lesionResults) {
            String toothCode = textValue(lesion, "toothCode", "tooth_code");
            if (StringUtils.hasText(toothCode)) {
                toothCodes.add(toothCode.trim());
            }
        }
        return toothCodes.size();
    }

    private String resolveErrorCode(AiAnalysisResultCallbackCommand callback) {
        String errorCode = trimToNull(callback.errorCode());
        if (errorCode != null) {
            return errorCode;
        }
        return textValue(callback.rawResultJson(), "errorCode", "error_code");
    }

    private JsonNode arrayValue(JsonNode root, String... fields) {
        if (root == null || root.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private Integer intValue(JsonNode root, String... fields) {
        if (root == null || root.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isInt()) {
                return value.asInt();
            }
        }
        return null;
    }

    private String textValue(JsonNode root, String... fields) {
        if (root == null || root.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private String fileNameFromObjectKey(String objectKey) {
        String normalized = objectKey.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private LocalDateTime defaultStartedAt(LocalDateTime startedAt) {
        return startedAt == null ? LocalDateTime.now() : startedAt;
    }

    private LocalDateTime defaultCompletedAt(LocalDateTime completedAt) {
        return completedAt == null ? LocalDateTime.now() : completedAt;
    }

    private String defaultRemark(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
