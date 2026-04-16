package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.CorrectionFeedbackDomainService;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectionFeedbackAppService {

    private final AnalysisCommandRepository analysisCommandRepository;
    private final AnaCorrectionFeedbackRepository anaCorrectionFeedbackRepository;
    private final CorrectionFeedbackDomainService correctionFeedbackDomainService;
    private final ObjectMapper objectMapper;

    public CorrectionFeedbackAppService(AnalysisCommandRepository analysisCommandRepository,
                                        AnaCorrectionFeedbackRepository anaCorrectionFeedbackRepository,
                                        CorrectionFeedbackDomainService correctionFeedbackDomainService,
                                        ObjectMapper objectMapper) {
        this.analysisCommandRepository = analysisCommandRepository;
        this.anaCorrectionFeedbackRepository = anaCorrectionFeedbackRepository;
        this.correctionFeedbackDomainService = correctionFeedbackDomainService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CorrectionFeedbackVO submit(SubmitCorrectionFeedbackCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(command.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());

        // Delegate rules to CorrectionFeedbackDomainService
        correctionFeedbackDomainService.ensureCaseAllowsCorrection(medicalCase.caseStatusCode());
        correctionFeedbackDomainService.validateFeedbackTypeCode(command.feedbackTypeCode());

        Long sourceAttachmentId = null;
        if (command.sourceImageId() != null) {
            AnalysisImageModel sourceImage = analysisCommandRepository.findImage(command.sourceImageId())
                    .filter(item -> item.caseId().equals(medicalCase.caseId()))
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Source image does not exist"));
            sourceAttachmentId = sourceImage.attachmentId();
        }
        long feedbackId = IdWorker.getId();
        anaCorrectionFeedbackRepository.save(new CorrectionFeedbackCreateModel(
                feedbackId,
                medicalCase.caseId(),
                command.diagnosisId(),
                command.sourceImageId(),
                sourceAttachmentId,
                operator.getUserId(),
                toJson(command.originalInferenceJson()),
                toJson(command.correctedTruthJson()),
                command.feedbackTypeCode().trim(),
                "1",
                null,
                "1",
                "0",
                "PENDING",
                null,
                null,
                medicalCase.orgId()));
        return new CorrectionFeedbackVO(feedbackId, medicalCase.caseId(), command.feedbackTypeCode().trim(), "1", null, "1", "0", "PENDING");
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
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
}


