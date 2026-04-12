package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.repository.AnalysisQueryRepository;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnalysisTaskAppService {

    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AnalysisCommandRepository analysisCommandRepository;
    private final AnalysisQueryRepository analysisQueryRepository;
    private final AnalysisTaskEventPublisher analysisTaskEventPublisher;
    private final CaseCommandAppService caseCommandAppService;
    private final AnalysisProperties analysisProperties;
    private final ObjectMapper objectMapper;

    public AnalysisTaskAppService(AnalysisCommandRepository analysisCommandRepository,
                                  AnalysisQueryRepository analysisQueryRepository,
                                  AnalysisTaskEventPublisher analysisTaskEventPublisher,
                                  CaseCommandAppService caseCommandAppService,
                                  AnalysisProperties analysisProperties,
                                  ObjectMapper objectMapper) {
        this.analysisCommandRepository = analysisCommandRepository;
        this.analysisQueryRepository = analysisQueryRepository;
        this.analysisTaskEventPublisher = analysisTaskEventPublisher;
        this.caseCommandAppService = caseCommandAppService;
        this.analysisProperties = analysisProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisTaskVO createTask(Long caseId, CreateAnalysisTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());
        if (!"QC_PENDING".equals(medicalCase.caseStatusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case is not ready for analysis");
        }

        List<Long> imageIds = command.imageIds().stream().distinct().toList();
        List<AnalysisImageModel> images = analysisCommandRepository.listImages(caseId, imageIds);
        if (images.size() != imageIds.size()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Selected images are invalid");
        }
        if (images.stream().anyMatch(item -> !"PASS".equals(item.qualityStatusCode()))) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Selected images are not quality-approved");
        }

        AnalysisPatientModel patient = analysisCommandRepository.findPatient(medicalCase.patientId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));

        long taskId = IdWorker.getId();
        String taskNo = buildTaskNo(taskId);
        String taskTypeCode = StringUtils.hasText(command.taskTypeCode()) ? command.taskTypeCode().trim() : "INFERENCE";
        String payloadJson = toJson(buildPayload(taskNo, taskTypeCode, medicalCase, patient, images));
        String modelVersion = analysisProperties.getDefaultModelVersion();

        analysisCommandRepository.createTask(new AnalysisTaskCreateModel(
                taskId,
                taskNo,
                medicalCase.caseId(),
                medicalCase.patientId(),
                modelVersion,
                taskTypeCode,
                "QUEUEING",
                payloadJson,
                medicalCase.orgId(),
                "ACTIVE",
                operator.getUserId()));
        analysisTaskEventPublisher.publishRequested(new AnalysisRequestedEvent(taskId, taskNo, "QUEUEING", payloadJson));
        caseCommandAppService.transitionStatus(caseId, new CaseStatusTransitionCommand(
                "ANALYZING",
                "QC_PASSED",
                "AI task created: " + taskNo));
        return new AnalysisTaskVO(taskId, taskNo, "QUEUEING", taskTypeCode, modelVersion, null, LocalDateTime.now(), null, null);
    }

    public AnalysisTaskVO getTask(Long taskId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel task = analysisQueryRepository.findTask(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));
        ensureOrgAccess(operator, task.orgId());
        return new AnalysisTaskVO(
                task.taskId(),
                task.taskNo(),
                task.taskStatusCode(),
                task.taskTypeCode(),
                task.modelVersion(),
                task.errorMessage(),
                task.createdAt(),
                task.startedAt(),
                task.completedAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private Map<String, Object> buildPayload(String taskNo,
                                             String taskTypeCode,
                                             AnalysisCaseModel medicalCase,
                                             AnalysisPatientModel patient,
                                             List<AnalysisImageModel> images) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskNo", taskNo);
        payload.put("taskTypeCode", taskTypeCode);
        payload.put("caseId", medicalCase.caseId());
        payload.put("patientId", medicalCase.patientId());
        payload.put("orgId", medicalCase.orgId());
        payload.put("modelVersion", analysisProperties.getDefaultModelVersion());
        payload.put("images", images.stream().map(item -> {
            Map<String, Object> image = new LinkedHashMap<>();
            image.put("imageId", item.imageId());
            image.put("attachmentId", item.attachmentId());
            image.put("imageTypeCode", item.imageTypeCode());
            image.put("bucketName", item.bucketName());
            image.put("objectKey", item.objectKey());
            return image;
        }).toList());
        Map<String, Object> patientProfile = new LinkedHashMap<>();
        patientProfile.put("age", patient.age());
        patientProfile.put("genderCode", patient.genderCode());
        payload.put("patientProfile", patientProfile);
        return payload;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private String buildTaskNo(long taskId) {
        return "TASK" + LocalDateTime.now().format(TASK_NO_FORMATTER) + String.format("%06d", taskId % 1_000_000);
    }
}
