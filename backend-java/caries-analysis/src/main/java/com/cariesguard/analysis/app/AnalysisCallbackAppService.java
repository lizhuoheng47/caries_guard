package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskStatusUpdateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.model.RiskAssessmentCreateModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.domain.repository.MedRiskAssessmentRecordRepository;
import com.cariesguard.analysis.domain.service.AnalysisCallbackDomainService;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.infrastructure.client.AiCallbackSignatureVerifier;
import com.cariesguard.analysis.interfaces.dto.AiAnalysisCallbackDTO;
import com.cariesguard.analysis.interfaces.dto.AiVisualAssetDTO;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ObjectMapper objectMapper;

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
        this.aiCallbackSignatureVerifier = aiCallbackSignatureVerifier;
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.anaResultSummaryRepository = anaResultSummaryRepository;
        this.anaVisualAssetRepository = anaVisualAssetRepository;
        this.medRiskAssessmentRecordRepository = medRiskAssessmentRecordRepository;
        this.analysisIdempotencyDomainService = analysisIdempotencyDomainService;
        this.analysisCallbackDomainService = analysisCallbackDomainService;
        this.analysisTaskEventPublisher = analysisTaskEventPublisher;
        this.caseCommandAppService = caseCommandAppService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisCallbackAckVO handleResultCallback(String rawBody, String timestamp, String signature) {
        aiCallbackSignatureVerifier.verify(rawBody, timestamp, signature);
        AiAnalysisCallbackDTO callback = readCallback(rawBody);
        String taskNo = analysisCallbackDomainService.normalizeAndValidateTaskNo(callback.taskNo());
        String incomingStatus = analysisCallbackDomainService.normalizeAndValidateTaskStatus(callback.taskStatusCode());
        AnalysisTaskViewModel task = anaTaskRecordRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));

        // Idempotent duplicate check
        if (analysisIdempotencyDomainService.isDuplicateTerminalCallback(task, incomingStatus)) {
            return new AnalysisCallbackAckVO(task.taskNo(), task.taskStatusCode(), true);
        }
        analysisIdempotencyDomainService.ensureCallbackAllowed(task, incomingStatus);

        // R-LATE-CALLBACK: skip write-back if task has already been retried
        if (analysisIdempotencyDomainService.hasBeenRetried(task.taskId())) {
            log.warn("Late callback on retried task taskNo={} incomingStatus={}, logging only", task.taskNo(), incomingStatus);
            return new AnalysisCallbackAckVO(task.taskNo(), incomingStatus, true);
        }

        return switch (incomingStatus) {
            case "PROCESSING" -> processProcessing(task, callback);
            case "SUCCESS" -> processSuccess(task, callback);
            case "FAILED" -> processFailure(task, callback);
            default -> throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        };
    }

    private AnalysisCallbackAckVO processProcessing(AnalysisTaskViewModel task, AiAnalysisCallbackDTO callback) {
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "PROCESSING",
                null,
                defaultStartedAt(callback.startedAt()),
                null));
        return new AnalysisCallbackAckVO(task.taskNo(), "PROCESSING", false);
    }

    private AnalysisCallbackAckVO processSuccess(AnalysisTaskViewModel task, AiAnalysisCallbackDTO callback) {
        // Delegate validation to domain service
        analysisCallbackDomainService.validateSuccessCallbackCompleteness(callback);

        // Extract summary aggregates for direct column storage (D2)
        AnalysisCallbackDomainService.SummaryAggregates aggregates =
                analysisCallbackDomainService.extractSummaryAggregates(callback.summary(), callback.rawResultJson());

        String rawResultJson = resolveRawResultJson(callback);
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "SUCCESS",
                null,
                defaultStartedAt(callback.startedAt()),
                defaultCompletedAt(callback.completedAt())));
        anaResultSummaryRepository.save(new AnalysisResultSummaryModel(
                IdWorker.getId(),
                task.taskId(),
                task.caseId(),
                rawResultJson,
                aggregates.overallHighestSeverity(),
                aggregates.uncertaintyScore(),
                aggregates.reviewSuggestedFlag(),
                task.orgId(),
                0L));
        anaVisualAssetRepository.replaceByTaskId(task.taskId(), buildVisualAssets(task, callback.visualAssets(), callback.modelVersion()));
        if (callback.riskAssessment() != null && StringUtils.hasText(callback.riskAssessment().overallRiskLevelCode())) {
            medRiskAssessmentRecordRepository.save(new RiskAssessmentCreateModel(
                    IdWorker.getId(),
                    task.caseId(),
                    task.patientId(),
                    callback.riskAssessment().overallRiskLevelCode().trim(),
                    toJson(callback.riskAssessment().assessmentReportJson()),
                    callback.riskAssessment().recommendedCycleDays(),
                    defaultCompletedAt(callback.completedAt()),
                    task.orgId(),
                    0L));
        }

        // Status transition via case module (R1: never directly update med_case)
        String targetStatus = analysisCallbackDomainService.resolveTargetCaseStatus("SUCCESS");
        String reasonCode = analysisCallbackDomainService.resolveChangeReasonCode("SUCCESS");
        caseCommandAppService.transitionStatusAsSystem(task.caseId(), task.orgId(), new CaseStatusTransitionCommand(
                targetStatus, reasonCode, "AI callback success: " + task.taskNo()));

        // Publish completed event
        analysisTaskEventPublisher.publishCompleted(new AnalysisCompletedEvent(
                task.taskId(), task.taskNo(), task.caseId(), task.modelVersion(),
                defaultCompletedAt(callback.completedAt())));

        return new AnalysisCallbackAckVO(task.taskNo(), "SUCCESS", false);
    }

    private AnalysisCallbackAckVO processFailure(AnalysisTaskViewModel task, AiAnalysisCallbackDTO callback) {
        anaTaskRecordRepository.updateStatus(new AnalysisTaskStatusUpdateModel(
                task.taskNo(),
                "FAILED",
                trimToNull(callback.errorMessage()),
                defaultStartedAt(callback.startedAt()),
                defaultCompletedAt(callback.completedAt())));

        // Status transition via case module (R1: never directly update med_case)
        String targetStatus = analysisCallbackDomainService.resolveTargetCaseStatus("FAILED");
        String reasonCode = analysisCallbackDomainService.resolveChangeReasonCode("FAILED");
        caseCommandAppService.transitionStatusAsSystem(task.caseId(), task.orgId(), new CaseStatusTransitionCommand(
                targetStatus, reasonCode,
                defaultRemark(callback.errorMessage(), "AI callback failed: " + task.taskNo())));

        // Publish failed event
        analysisTaskEventPublisher.publishFailed(new AnalysisFailedEvent(
                task.taskId(), task.taskNo(), task.caseId(),
                trimToNull(callback.errorMessage()),
                defaultCompletedAt(callback.completedAt())));

        return new AnalysisCallbackAckVO(task.taskNo(), "FAILED", false);
    }

    private AiAnalysisCallbackDTO readCallback(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, AiAnalysisCallbackDTO.class);
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "AI callback payload is invalid");
        }
    }

    private String resolveRawResultJson(AiAnalysisCallbackDTO callback) {
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
        return visualAssets.stream().map(item -> new AnalysisVisualAssetCreateModel(
                IdWorker.getId(),
                task.taskId(),
                task.caseId(),
                resolvedModelVersion,
                item.assetTypeCode().trim(),
                item.attachmentId(),
                task.orgId(),
                0L)).toList();
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
