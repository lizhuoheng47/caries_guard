package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackExportCandidateModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.CorrectionFeedbackDomainService;
import com.cariesguard.analysis.interfaces.command.ExportCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.ReviewCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackExportSampleVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackExportVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackReviewVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionReasonCategoryLabels;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CorrectionFeedbackAppService {

    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> SUPPORTED_REVIEW_STATUS = Set.of("PENDING", "APPROVED", "REJECTED");

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
        String trainingCandidateFlag = Boolean.FALSE.equals(command.trainingCandidate()) ? "0" : "1";
        String correctedTruthJson = toJson(enrichedCorrectedTruth(command));
        anaCorrectionFeedbackRepository.save(new CorrectionFeedbackCreateModel(
                feedbackId,
                medicalCase.caseId(),
                command.diagnosisId(),
                command.sourceImageId(),
                sourceAttachmentId,
                operator.getUserId(),
                toJson(command.originalInferenceJson()),
                correctedTruthJson,
                command.feedbackTypeCode().trim(),
                "1",
                null,
                trainingCandidateFlag,
                "0",
                "PENDING",
                null,
                null,
                medicalCase.orgId()));
        String correctionReasonCategory = trimToNull(command.correctionReasonCategory());
        return new CorrectionFeedbackVO(
                feedbackId,
                medicalCase.caseId(),
                command.feedbackTypeCode().trim(),
                trimToNull(command.originalAiGrade()),
                trimToNull(command.doctorCorrectedGrade()),
                command.originalUncertainty(),
                command.acceptedAiConclusion(),
                trimToNull(command.correctionReason()),
                "1",
                null,
                trainingCandidateFlag,
                "0",
                "PENDING",
                trimToNull(command.doctorConfirmedGrade()),
                command.agreedWithAi(),
                correctionReasonCategory,
                CorrectionReasonCategoryLabels.toLabel(correctionReasonCategory),
                command.agreedWithAiExplanation(),
                trimToNull(command.followUpSuggestion()));
    }

    @Transactional
    public CorrectionFeedbackExportVO exportTrainingCandidates(ExportCorrectionFeedbackCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        int limit = normalizeLimit(command == null ? null : command.limit());
        Long orgFilter = operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
        List<CorrectionFeedbackExportCandidateModel> candidates =
                anaCorrectionFeedbackRepository.listTrainingCandidates(orgFilter, limit);
        if (candidates.isEmpty()) {
            return new CorrectionFeedbackExportVO(null, 0, List.of());
        }

        String snapshotNo = newSnapshotNo();
        List<CorrectionFeedbackExportSampleVO> samples = candidates.stream()
                .map(this::toExportSample)
                .toList();
        anaCorrectionFeedbackRepository.markExported(
                candidates.stream().map(CorrectionFeedbackExportCandidateModel::feedbackId).toList(),
                snapshotNo);
        return new CorrectionFeedbackExportVO(snapshotNo, samples.size(), samples);
    }

    @Transactional
    public CorrectionFeedbackReviewVO review(ReviewCorrectionFeedbackCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        List<Long> feedbackIds = normalizeFeedbackIds(command.feedbackIds());
        if (feedbackIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Feedback ids must not be empty");
        }
        String reviewStatusCode = normalizeReviewStatus(command.reviewStatusCode());
        String trainingCandidateFlag = command.trainingCandidate() == null
                ? null
                : (command.trainingCandidate() ? "1" : "0");
        Long orgFilter = operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
        int reviewedCount = anaCorrectionFeedbackRepository.reviewFeedbacks(
                feedbackIds,
                reviewStatusCode,
                trainingCandidateFlag,
                operator.getUserId(),
                LocalDateTime.now(),
                orgFilter);
        return new CorrectionFeedbackReviewVO(reviewedCount, reviewStatusCode, trainingCandidateFlag);
    }

    private JsonNode enrichedCorrectedTruth(SubmitCorrectionFeedbackCommand command) {
        ObjectNode root;
        if (command.correctedTruthJson() != null && command.correctedTruthJson().isObject()) {
            root = command.correctedTruthJson().deepCopy();
        } else {
            root = objectMapper.createObjectNode();
            if (command.correctedTruthJson() != null && !command.correctedTruthJson().isNull()) {
                root.set("correctedTruth", command.correctedTruthJson());
            }
        }
        ObjectNode governance = root.putObject("feedbackGovernance");
        putText(governance, "originalAiGrade", command.originalAiGrade());
        putText(governance, "doctorCorrectedGrade", command.doctorCorrectedGrade());
        if (command.originalUncertainty() != null) {
            governance.put("originalUncertainty", command.originalUncertainty());
        }
        if (command.acceptedAiConclusion() != null) {
            governance.put("acceptedAiConclusion", command.acceptedAiConclusion());
        }
        putText(governance, "correctionReason", command.correctionReason());
        governance.put("trainingCandidate", !Boolean.FALSE.equals(command.trainingCandidate()));
        putText(governance, "doctorConfirmedGrade", command.doctorConfirmedGrade());
        if (command.agreedWithAi() != null) {
            governance.put("agreedWithAi", command.agreedWithAi());
        }
        putText(governance, "correctionReasonCategory", command.correctionReasonCategory());
        if (command.agreedWithAiExplanation() != null) {
            governance.put("agreedWithAiExplanation", command.agreedWithAiExplanation());
        }
        putText(governance, "followUpSuggestion", command.followUpSuggestion());
        governance.put("schemaVersion", "feedback-governance-v2");
        return root;
    }

    private CorrectionFeedbackExportSampleVO toExportSample(CorrectionFeedbackExportCandidateModel model) {
        JsonNode originalInference = readJson(model.originalInferenceJson());
        JsonNode correctedTruth = readJson(model.correctedTruthJson());
        JsonNode governance = correctedTruth.path("feedbackGovernance");
        return new CorrectionFeedbackExportSampleVO(
                model.feedbackId(),
                model.caseId(),
                model.diagnosisId(),
                model.sourceImageId(),
                model.sourceAttachmentId(),
                model.feedbackTypeCode(),
                model.reviewStatusCode(),
                firstText(governance, "originalAiGrade", originalInference, "gradingLabel"),
                firstText(governance, "doctorCorrectedGrade", correctedTruth, "gradingLabel"),
                doubleValue(governance, "originalUncertainty"),
                booleanValue(governance, "acceptedAiConclusion"),
                textValue(governance, "correctionReason"),
                textValue(governance, "schemaVersion"));
    }

    private JsonNode readJson(String value) {
        if (!StringUtils.hasText(value)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String firstText(JsonNode firstNode, String firstField, JsonNode secondNode, String secondField) {
        String first = textValue(firstNode, firstField);
        if (first != null) {
            return first;
        }
        return textValue(secondNode, secondField);
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText(null);
        return trimToNull(value);
    }

    private Double doubleValue(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field) || !node.get(field).isNumber()) {
            return null;
        }
        return node.get(field).doubleValue();
    }

    private Boolean booleanValue(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field) || !node.get(field).isBoolean()) {
            return null;
        }
        return node.get(field).booleanValue();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        return Math.max(1, Math.min(limit, 500));
    }

    private List<Long> normalizeFeedbackIds(List<Long> feedbackIds) {
        if (feedbackIds == null) {
            return List.of();
        }
        return feedbackIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(500)
                .toList();
    }

    private String normalizeReviewStatus(String reviewStatusCode) {
        String normalized = trimToNull(reviewStatusCode);
        if (normalized == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Review status must not be empty");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_REVIEW_STATUS.contains(normalized)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Unsupported review status");
        }
        return normalized;
    }

    private String newSnapshotNo() {
        return "FBDS-" + SNAPSHOT_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + IdWorker.getIdStr();
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

    private void putText(ObjectNode node, String field, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            node.put(field, trimmed);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}


