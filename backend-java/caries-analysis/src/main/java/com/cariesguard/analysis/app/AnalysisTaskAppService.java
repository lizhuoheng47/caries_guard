package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.command.RetryAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.dto.AiAnalysisRequestDTO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnalysisTaskAppService {

    private final AnalysisCommandRepository analysisCommandRepository;
    private final AnaTaskRecordRepository anaTaskRecordRepository;
    private final AnalysisTaskEventPublisher analysisTaskEventPublisher;
    private final AnalysisTaskDomainService analysisTaskDomainService;
    private final AnalysisIdempotencyDomainService analysisIdempotencyDomainService;
    private final CaseCommandAppService caseCommandAppService;
    private final AnalysisProperties analysisProperties;
    private final ObjectMapper objectMapper;

    public AnalysisTaskAppService(AnalysisCommandRepository analysisCommandRepository,
                                  AnaTaskRecordRepository anaTaskRecordRepository,
                                  AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                  AnalysisTaskDomainService analysisTaskDomainService,
                                  AnalysisIdempotencyDomainService analysisIdempotencyDomainService,
                                  CaseCommandAppService caseCommandAppService,
                                  AnalysisProperties analysisProperties,
                                  ObjectMapper objectMapper) {
        this.analysisCommandRepository = analysisCommandRepository;
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.analysisTaskEventPublisher = analysisTaskEventPublisher;
        this.analysisTaskDomainService = analysisTaskDomainService;
        this.analysisIdempotencyDomainService = analysisIdempotencyDomainService;
        this.caseCommandAppService = caseCommandAppService;
        this.analysisProperties = analysisProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisTaskVO createTask(CreateAnalysisTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(command.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());

        // Domain rules — delegated to AnalysisTaskDomainService
        analysisTaskDomainService.ensureCaseReadyForAnalysis(medicalCase.caseStatusCode());
        analysisTaskDomainService.ensurePatientMatchesCase(command.patientId(), medicalCase.patientId());
        analysisTaskDomainService.ensureNoRunningTask(
                anaTaskRecordRepository.existsRunningTaskByCaseId(command.caseId()),
                Boolean.TRUE.equals(command.forceRetryFlag()));

        List<AnalysisImageModel> images = analysisCommandRepository.listCaseImages(command.caseId()).stream()
                .filter(item -> "PASS".equals(item.qualityStatusCode()))
                .toList();
        analysisTaskDomainService.ensureAnalyzableImagesExist(images);

        AnalysisPatientModel patient = analysisCommandRepository.findPatient(medicalCase.patientId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));

        long taskId = IdWorker.getId();
        String taskNo = analysisTaskDomainService.generateTaskNo(taskId);
        String taskTypeCode = analysisTaskDomainService.resolveTaskTypeCode(command.taskTypeCode());
        String modelVersion = analysisProperties.getDefaultModelVersion();
        String payloadJson = toJson(buildRequestDto(taskNo, taskTypeCode, medicalCase, patient, images, modelVersion));

        anaTaskRecordRepository.save(new AnalysisTaskCreateModel(
                taskId, taskNo, medicalCase.caseId(), medicalCase.patientId(),
                modelVersion, taskTypeCode, "QUEUEING", payloadJson,
                medicalCase.orgId(), "ACTIVE", operator.getUserId(),
                null));  // retryFromTaskId = null for first-time task
        caseCommandAppService.transitionStatus(command.caseId(), new CaseStatusTransitionCommand(
                "ANALYZING", "QC_PASSED",
                defaultRemark(command.remark(), "AI task created: " + taskNo)));
        analysisTaskEventPublisher.publishRequested(new AnalysisRequestedEvent(taskId, taskNo, "QUEUEING", payloadJson));
        return new AnalysisTaskVO(taskId, taskNo, "QUEUEING", taskTypeCode, modelVersion, null, LocalDateTime.now(), null, null);
    }

    /**
     * Retry a FAILED analysis task. Creates a new task linked to the original via retryFromTaskId.
     * R-RETRY rules:
     * 1. Only FAILED tasks may be retried
     * 2. Retry creates a new task (new taskId + taskNo), never overwrites
     * 3. New task links via retryFromTaskId for audit trail
     */
    @Transactional
    public AnalysisTaskVO retryTask(RetryAnalysisTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel originalTask = anaTaskRecordRepository.findById(command.taskId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Original task does not exist"));
        ensureOrgAccess(operator, originalTask.orgId());

        // R-RETRY rule 1: Only FAILED allowed
        analysisIdempotencyDomainService.ensureRetryAllowed(originalTask);

        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(originalTask.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));

        List<AnalysisImageModel> images = analysisCommandRepository.listCaseImages(originalTask.caseId()).stream()
                .filter(item -> "PASS".equals(item.qualityStatusCode()))
                .toList();
        analysisTaskDomainService.ensureAnalyzableImagesExist(images);

        AnalysisPatientModel patient = analysisCommandRepository.findPatient(originalTask.patientId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));

        // R-RETRY rule 2: New task (new taskId + taskNo)
        long newTaskId = IdWorker.getId();
        String newTaskNo = analysisTaskDomainService.generateTaskNo(newTaskId);
        String taskTypeCode = originalTask.taskTypeCode();
        String modelVersion = analysisProperties.getDefaultModelVersion();
        String payloadJson = toJson(buildRequestDto(newTaskNo, taskTypeCode, medicalCase, patient, images, modelVersion));

        // R-RETRY rule 3: retryFromTaskId links to original
        anaTaskRecordRepository.save(new AnalysisTaskCreateModel(
                newTaskId, newTaskNo, medicalCase.caseId(), medicalCase.patientId(),
                modelVersion, taskTypeCode, "QUEUEING", payloadJson,
                medicalCase.orgId(), "ACTIVE", operator.getUserId(),
                originalTask.taskId()));  // retryFromTaskId = original failed task
        String reasonCode = analysisTaskDomainService.resolveRetryReasonCode(command.reasonCode());
        caseCommandAppService.transitionStatus(medicalCase.caseId(), new CaseStatusTransitionCommand(
                "ANALYZING", reasonCode,
                defaultRemark(command.reasonRemark(), "Retry from task: " + originalTask.taskNo())));
        analysisTaskEventPublisher.publishRequested(new AnalysisRequestedEvent(newTaskId, newTaskNo, "QUEUEING", payloadJson));
        return new AnalysisTaskVO(newTaskId, newTaskNo, "QUEUEING", taskTypeCode, modelVersion, null, LocalDateTime.now(), null, null);
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private AiAnalysisRequestDTO buildRequestDto(String taskNo,
                                                 String taskTypeCode,
                                                 AnalysisCaseModel medicalCase,
                                                 AnalysisPatientModel patient,
                                                 List<AnalysisImageModel> images,
                                                 String modelVersion) {
        return new AiAnalysisRequestDTO(
                taskNo,
                taskTypeCode,
                medicalCase.caseId(),
                medicalCase.patientId(),
                medicalCase.orgId(),
                modelVersion,
                images.stream().map(item -> new AiAnalysisRequestDTO.ImageItem(
                        item.imageId(),
                        item.attachmentId(),
                        item.imageTypeCode(),
                        item.bucketName(),
                        item.objectKey())).toList(),
                new AiAnalysisRequestDTO.PatientProfile(patient.age(), patient.genderCode()));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private String defaultRemark(String remark, String fallback) {
        return StringUtils.hasText(remark) ? remark.trim() : fallback;
    }
}
