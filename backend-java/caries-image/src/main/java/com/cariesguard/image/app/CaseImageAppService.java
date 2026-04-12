package com.cariesguard.image.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageAssociationModel;
import com.cariesguard.image.domain.model.ImageManagedModel;
import com.cariesguard.image.domain.model.ImageQualityCheckCreateModel;
import com.cariesguard.image.domain.model.ImageQualityCheckModel;
import com.cariesguard.image.domain.model.ImageViewModel;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.interfaces.command.CreateCaseImageCommand;
import com.cariesguard.image.interfaces.command.SaveImageQualityCheckCommand;
import com.cariesguard.image.interfaces.vo.CaseImageMutationVO;
import com.cariesguard.image.interfaces.vo.ImageQualityCheckVO;
import com.cariesguard.image.interfaces.vo.ImageQualityCheckSummaryVO;
import com.cariesguard.image.interfaces.vo.ImageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CaseImageAppService {

    private final ImageCommandRepository imageCommandRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final ObjectMapper objectMapper;
    private final CaseCommandAppService caseCommandAppService;

    public CaseImageAppService(ImageCommandRepository imageCommandRepository,
                               ImageQueryRepository imageQueryRepository,
                               ObjectMapper objectMapper,
                               CaseCommandAppService caseCommandAppService) {
        this.imageCommandRepository = imageCommandRepository;
        this.imageQueryRepository = imageQueryRepository;
        this.objectMapper = objectMapper;
        this.caseCommandAppService = caseCommandAppService;
    }

    @Transactional
    public CaseImageMutationVO createCaseImage(Long caseId, CreateCaseImageCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AttachmentOwnerCaseModel medicalCase = loadCase(caseId);
        ensureOrgAccess(operator, medicalCase.orgId());
        AttachmentViewModel attachment = imageCommandRepository.findAttachment(command.attachmentId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Attachment does not exist"));
        if (!attachment.orgId().equals(medicalCase.orgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (!medicalCase.patientId().equals(command.patientId()) || !medicalCase.visitId().equals(command.visitId())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case patient or visit mismatch");
        }

        String imageTypeCode = defaultImageType(command.imageTypeCode());
        if ("1".equals(defaultPrimaryFlag(command.primaryFlag()))) {
            imageCommandRepository.clearPrimaryFlag(caseId, imageTypeCode, operator.getUserId());
        }
        long imageId = IdWorker.getId();
        imageCommandRepository.createImage(new ImageAssociationModel(
                imageId,
                caseId,
                command.visitId(),
                command.patientId(),
                command.attachmentId(),
                imageTypeCode,
                defaultImageSource(command.imageSourceCode()),
                command.shootingTime(),
                trimToNull(command.bodyPositionCode()),
                imageCommandRepository.nextImageIndexNo(caseId, imageTypeCode),
                "PENDING",
                defaultPrimaryFlag(command.primaryFlag()),
                medicalCase.orgId(),
                "ACTIVE",
                trimToNull(command.remark()),
                operator.getUserId()));
        transitionCaseToQcPendingIfNeeded(caseId, medicalCase);
        return new CaseImageMutationVO(imageId, "PENDING");
    }

    public List<ImageVO> listCaseImages(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AttachmentOwnerCaseModel medicalCase = loadCase(caseId);
        ensureOrgAccess(operator, medicalCase.orgId());
        return imageQueryRepository.listImagesByCaseId(caseId).stream().map(this::toImageVO).toList();
    }

    public ImageVO getImage(Long imageId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ImageManagedModel image = imageCommandRepository.findImage(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Image does not exist"));
        ensureOrgAccess(operator, image.orgId());
        return imageQueryRepository.findImage(imageId)
                .map(this::toImageVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Image does not exist"));
    }

    @Transactional
    public ImageQualityCheckVO saveQualityCheck(Long imageId, SaveImageQualityCheckCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ImageManagedModel image = imageCommandRepository.findImage(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Image does not exist"));
        ensureOrgAccess(operator, image.orgId());

        LocalDateTime checkedAt = LocalDateTime.now();
        imageCommandRepository.saveQualityCheck(new ImageQualityCheckCreateModel(
                IdWorker.getId(),
                imageId,
                image.caseId(),
                image.patientId(),
                defaultCheckType(command.checkTypeCode()),
                defaultCheckResult(command.checkResultCode()),
                command.qualityScore(),
                command.blurScore(),
                command.exposureScore(),
                command.integrityScore(),
                command.occlusionScore(),
                toJson(command.issueCodes()),
                trimToNull(command.suggestionText()),
                "1",
                operator.getUserId(),
                checkedAt,
                image.orgId(),
                "ACTIVE",
                trimToNull(command.remark()),
                operator.getUserId()));
        return getCurrentQualityCheck(imageId);
    }

    public ImageQualityCheckVO getCurrentQualityCheck(Long imageId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ImageManagedModel image = imageCommandRepository.findImage(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Image does not exist"));
        ensureOrgAccess(operator, image.orgId());
        ImageQualityCheckModel qualityCheck = imageQueryRepository.findCurrentQualityCheck(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Quality check does not exist"));
        return new ImageQualityCheckVO(
                qualityCheck.imageId(),
                qualityCheck.checkTypeCode(),
                qualityCheck.checkResultCode(),
                qualityCheck.qualityScore(),
                qualityCheck.blurScore(),
                qualityCheck.exposureScore(),
                qualityCheck.integrityScore(),
                qualityCheck.occlusionScore(),
                readIssueCodes(qualityCheck.issueCodesJson()),
                qualityCheck.suggestionText(),
                qualityCheck.checkedAt());
    }

    private AttachmentOwnerCaseModel loadCase(Long caseId) {
        return imageCommandRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private ImageVO toImageVO(ImageViewModel image) {
        ImageQualityCheckSummaryVO qualityCheck = imageQueryRepository.findCurrentQualityCheck(image.imageId())
                .map(this::toQualityCheckSummary)
                .orElse(null);
        return new ImageVO(
                image.imageId(),
                image.attachmentId(),
                image.originalName(),
                image.bucketName(),
                image.objectKey(),
                image.imageTypeCode(),
                image.imageSourceCode(),
                image.qualityStatusCode(),
                image.primaryFlag(),
                image.shootingTime(),
                image.bodyPositionCode(),
                qualityCheck);
    }

    private ImageQualityCheckSummaryVO toQualityCheckSummary(ImageQualityCheckModel qualityCheck) {
        return new ImageQualityCheckSummaryVO(
                qualityCheck.checkTypeCode(),
                qualityCheck.checkResultCode(),
                qualityCheck.qualityScore(),
                readIssueCodes(qualityCheck.issueCodesJson()),
                qualityCheck.checkedAt());
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private List<String> readIssueCodes(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Issue codes payload is invalid");
        }
    }

    private String defaultImageType(String imageTypeCode) {
        return StringUtils.hasText(imageTypeCode) ? imageTypeCode.trim() : "PANORAMIC";
    }

    private String defaultImageSource(String imageSourceCode) {
        return StringUtils.hasText(imageSourceCode) ? imageSourceCode.trim() : "UPLOAD";
    }

    private String defaultPrimaryFlag(String primaryFlag) {
        return StringUtils.hasText(primaryFlag) ? primaryFlag.trim() : "0";
    }

    private String defaultCheckType(String checkTypeCode) {
        return StringUtils.hasText(checkTypeCode) ? checkTypeCode.trim() : "AUTO";
    }

    private String defaultCheckResult(String checkResultCode) {
        return StringUtils.hasText(checkResultCode) ? checkResultCode.trim() : "REVIEW";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void transitionCaseToQcPendingIfNeeded(Long caseId, AttachmentOwnerCaseModel medicalCase) {
        if (!"CREATED".equals(medicalCase.caseStatusCode())) {
            return;
        }
        caseCommandAppService.transitionStatus(caseId, new CaseStatusTransitionCommand(
                "QC_PENDING",
                "IMAGE_UPLOADED",
                "First active image uploaded"));
    }
}
