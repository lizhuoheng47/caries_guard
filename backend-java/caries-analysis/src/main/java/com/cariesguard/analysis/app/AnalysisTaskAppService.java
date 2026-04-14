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
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AttachmentAppService attachmentAppService;
    private final AnalysisProperties analysisProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalysisTaskAppService(AnalysisCommandRepository analysisCommandRepository,
                                  AnaTaskRecordRepository anaTaskRecordRepository,
                                  AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                  AnalysisTaskDomainService analysisTaskDomainService,
                                  AnalysisIdempotencyDomainService analysisIdempotencyDomainService,
                                  CaseCommandAppService caseCommandAppService,
                                  AttachmentAppService attachmentAppService,
                                  AnalysisProperties analysisProperties,
                                  ObjectMapper objectMapper) {
        this.analysisCommandRepository = analysisCommandRepository;
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.analysisTaskEventPublisher = analysisTaskEventPublisher;
        this.analysisTaskDomainService = analysisTaskDomainService;
        this.analysisIdempotencyDomainService = analysisIdempotencyDomainService;
        this.caseCommandAppService = caseCommandAppService;
        this.attachmentAppService = attachmentAppService;
        this.analysisProperties = analysisProperties;
        this.objectMapper = objectMapper;
    }

    public AnalysisTaskAppService(AnalysisCommandRepository analysisCommandRepository,
                                  AnaTaskRecordRepository anaTaskRecordRepository,
                                  AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                  AnalysisTaskDomainService analysisTaskDomainService,
                                  AnalysisIdempotencyDomainService analysisIdempotencyDomainService,
                                  CaseCommandAppService caseCommandAppService,
                                  AnalysisProperties analysisProperties,
                                  ObjectMapper objectMapper) {
        this(analysisCommandRepository, anaTaskRecordRepository, analysisTaskEventPublisher,
                analysisTaskDomainService, analysisIdempotencyDomainService, caseCommandAppService,
                null, analysisProperties, objectMapper);
    }

    @Transactional
    public AnalysisTaskVO createTask(CreateAnalysisTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(command.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());

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
                null));
        caseCommandAppService.transitionStatus(command.caseId(), new CaseStatusTransitionCommand(
                "ANALYZING", "QC_PASSED",
                defaultRemark(command.remark(), "AI task created: " + taskNo)));
        analysisTaskEventPublisher.publishRequested(new AnalysisRequestedEvent(taskId, taskNo, "QUEUEING", payloadJson));
        return new AnalysisTaskVO(taskId, taskNo, "QUEUEING", taskTypeCode, modelVersion, null, LocalDateTime.now(), null, null, null, null);
    }

    @Transactional
    public AnalysisTaskVO retryTask(RetryAnalysisTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel originalTask = anaTaskRecordRepository.findById(command.taskId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Original task does not exist"));
        ensureOrgAccess(operator, originalTask.orgId());

        analysisIdempotencyDomainService.ensureRetryAllowed(originalTask);

        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(originalTask.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));

        List<AnalysisImageModel> images = analysisCommandRepository.listCaseImages(originalTask.caseId()).stream()
                .filter(item -> "PASS".equals(item.qualityStatusCode()))
                .toList();
        analysisTaskDomainService.ensureAnalyzableImagesExist(images);

        AnalysisPatientModel patient = analysisCommandRepository.findPatient(originalTask.patientId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));

        long newTaskId = IdWorker.getId();
        String newTaskNo = analysisTaskDomainService.generateTaskNo(newTaskId);
        String taskTypeCode = originalTask.taskTypeCode();
        String modelVersion = analysisProperties.getDefaultModelVersion();
        String payloadJson = toJson(buildRequestDto(newTaskNo, taskTypeCode, medicalCase, patient, images, modelVersion));

        anaTaskRecordRepository.save(new AnalysisTaskCreateModel(
                newTaskId, newTaskNo, medicalCase.caseId(), medicalCase.patientId(),
                modelVersion, taskTypeCode, "QUEUEING", payloadJson,
                medicalCase.orgId(), "ACTIVE", operator.getUserId(),
                originalTask.taskId()));
        String reasonCode = analysisTaskDomainService.resolveRetryReasonCode(command.reasonCode());
        caseCommandAppService.transitionStatus(medicalCase.caseId(), new CaseStatusTransitionCommand(
                "ANALYZING", reasonCode,
                defaultRemark(command.reasonRemark(), "Retry from task: " + originalTask.taskNo())));
        analysisTaskEventPublisher.publishRequested(new AnalysisRequestedEvent(newTaskId, newTaskNo, "QUEUEING", payloadJson));
        return new AnalysisTaskVO(newTaskId, newTaskNo, "QUEUEING", taskTypeCode, modelVersion, null, LocalDateTime.now(), null, null, null, null);
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
                images.stream().map(this::toRequestImageItem).toList(),
                new AiAnalysisRequestDTO.PatientProfile(patient.age(), patient.genderCode()));
    }

    private AiAnalysisRequestDTO.ImageItem toRequestImageItem(AnalysisImageModel item) {
        AttachmentAccessVO access = item.attachmentId() == null || attachmentAppService == null ? null : attachmentAppService.createInternalAccessUrl(item.attachmentId());
        String localStoragePath = attachmentAppService != null && "LOCAL_FS".equalsIgnoreCase(item.storageProviderCode())
                ? attachmentAppService.resolveLocalStoragePath(item.bucketName(), item.objectKey())
                : null;
        return new AiAnalysisRequestDTO.ImageItem(
                item.imageId(),
                item.attachmentId(),
                item.imageTypeCode(),
                item.bucketName(),
                item.objectKey(),
                item.storageProviderCode(),
                item.attachmentMd5(),
                access == null ? null : access.accessUrl(),
                access == null ? null : access.expireAt(),
                localStoragePath);
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


