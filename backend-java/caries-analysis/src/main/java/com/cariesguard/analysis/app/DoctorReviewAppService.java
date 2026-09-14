package com.cariesguard.analysis.app;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaReviewDraftRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.CorrectionFeedbackDomainService;
import com.cariesguard.analysis.infrastructure.dataobject.AnaReviewDraftDO;
import com.cariesguard.analysis.interfaces.command.SaveReviewDraftCommand;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.analysis.interfaces.vo.ReviewDraftVO;
import com.cariesguard.analysis.interfaces.vo.ReviewTaskDetailVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DoctorReviewAppService {
    private static final Set<String> GRADES = Set.of("C0", "C1", "C2", "C3", "C4", "G0", "G1", "G2", "G3", "G4");

    private final AnaTaskRecordRepository taskRepository;
    private final AnalysisCommandRepository analysisCommandRepository;
    private final AnaReviewDraftRepository draftRepository;
    private final AnalysisQueryAppService analysisQueryAppService;
    private final CorrectionFeedbackAppService correctionFeedbackAppService;
    private final CorrectionFeedbackDomainService correctionFeedbackDomainService;
    private final ObjectMapper objectMapper;

    public DoctorReviewAppService(AnaTaskRecordRepository taskRepository,
                                  AnalysisCommandRepository analysisCommandRepository,
                                  AnaReviewDraftRepository draftRepository,
                                  AnalysisQueryAppService analysisQueryAppService,
                                  CorrectionFeedbackAppService correctionFeedbackAppService,
                                  CorrectionFeedbackDomainService correctionFeedbackDomainService,
                                  ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.analysisCommandRepository = analysisCommandRepository;
        this.draftRepository = draftRepository;
        this.analysisQueryAppService = analysisQueryAppService;
        this.correctionFeedbackAppService = correctionFeedbackAppService;
        this.correctionFeedbackDomainService = correctionFeedbackDomainService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReviewDraftVO saveDraft(String taskIdentifier, SaveReviewDraftCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel task = resolveTask(taskIdentifier);
        AnalysisCaseModel medicalCase = accessibleCase(task, operator);
        correctionFeedbackDomainService.ensureCaseAllowsCorrection(medicalCase.caseStatusCode());

        AnaReviewDraftDO draft = new AnaReviewDraftDO();
        draft.setTaskId(task.taskId());
        draft.setCaseId(task.caseId());
        draft.setDoctorUserId(operator.getUserId());
        draft.setRevisedGrade(normalizeGrade(command.revisedGrade()));
        draft.setRevisedDetectionsJson(writeJson(normalizeDetections(command.revisedDetections())));
        draft.setReasonTagsJson(writeJson(normalizeTags(command.reasonTags())));
        draft.setNote(trimToNull(command.note()));
        draft.setOrgId(medicalCase.orgId());
        // 同一医生对同一任务只保留一份草稿，仓储层负责插入或覆盖，避免自动保存产生重复记录。
        return toVO(draftRepository.saveDraft(draft));
    }

    @Transactional
    public CorrectionFeedbackVO submit(String taskIdentifier, SaveReviewDraftCommand command) {
        // 正式提交前先持久化页面上的最后一次修改，保证草稿与纠正反馈使用同一份数据。
        ReviewDraftVO saved = saveDraft(taskIdentifier, command);
        if ("SUBMITTED".equals(saved.statusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Review has already been submitted");
        }

        AnalysisTaskViewModel task = resolveTask(taskIdentifier);
        AnalysisTaskDetailVO detail = analysisQueryAppService.getTaskDetail(task.taskId());
        AnalysisSummaryVO summary = detail.summary();
        AnalysisImageModel image = analysisCommandRepository.listCaseImages(task.caseId()).stream().findFirst().orElse(null);
        String originalGrade = summary == null ? null : summary.gradingLabel();
        boolean agreed = originalGrade != null && originalGrade.equalsIgnoreCase(saved.revisedGrade());
        ObjectNode correctedTruth = objectMapper.createObjectNode();
        correctedTruth.put("gradingLabel", saved.revisedGrade());
        correctedTruth.set("detections", objectMapper.valueToTree(saved.revisedDetections()));
        correctedTruth.set("reasonTags", objectMapper.valueToTree(saved.reasonTags()));
        if (saved.note() != null) {
            correctedTruth.put("note", saved.note());
        }

        CorrectionFeedbackVO feedback = correctionFeedbackAppService.submit(new SubmitCorrectionFeedbackCommand(
                task.caseId(),
                null,
                image == null ? null : image.imageId(),
                "RE_GRADE",
                summary == null ? null : summary.rawResultJson(),
                correctedTruth,
                originalGrade,
                saved.revisedGrade(),
                summary == null ? null : summary.uncertaintyScore(),
                agreed,
                StringUtils.collectionToDelimitedString(saved.reasonTags(), ",") + (saved.note() == null ? "" : ": " + saved.note()),
                true,
                saved.revisedGrade(),
                agreed,
                saved.reasonTags().isEmpty() ? "OTHER" : saved.reasonTags().get(0),
                agreed,
                null));
        // 先成功创建反馈再标记草稿已提交；事务异常时两处写入会一起回滚。
        draftRepository.markSubmitted(saved.draftId(), feedback.feedbackId(), operator().getUserId());
        return feedback;
    }

    public ReviewDraftVO getCurrentDraft(Long taskId) {
        AuthenticatedUser operator = operator();
        AnalysisTaskViewModel task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));
        accessibleCase(task, operator);
        return draftRepository.findByTaskAndDoctor(taskId, operator.getUserId()).map(this::toVO).orElse(null);
    }

    private AnalysisTaskViewModel resolveTask(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Task identifier is required");
        }
        return value.chars().allMatch(Character::isDigit)
                ? taskRepository.findById(Long.valueOf(value)).orElseThrow(() -> taskNotFound())
                : taskRepository.findByTaskNo(value).orElseThrow(() -> taskNotFound());
    }

    private AnalysisCaseModel accessibleCase(AnalysisTaskViewModel task, AuthenticatedUser operator) {
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(task.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !medicalCase.orgId().equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return medicalCase;
    }

    private ReviewDraftVO toVO(AnaReviewDraftDO draft) {
        return new ReviewDraftVO(
                draft.getId(), draft.getTaskId(), draft.getVersionNo(), draft.getRevisedGrade(),
                readDetections(draft.getRevisedDetectionsJson()), readTags(draft.getReasonTagsJson()),
                draft.getNote(), draft.getDraftStatusCode(), draft.getSubmittedFeedbackId(), draft.getUpdatedAt());
    }

    private List<ReviewTaskDetailVO.DetectionBoxVO> readDetections(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private List<String> readTags(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private List<ReviewTaskDetailVO.DetectionBoxVO> normalizeDetections(List<SaveReviewDraftCommand.DetectionBox> detections) {
        if (detections == null) return List.of();
        return detections.stream().map(item -> {
            ReviewTaskDetailVO.DetectionBoxVO box = new ReviewTaskDetailVO.DetectionBoxVO();
            box.setId(trimToNull(item.id()));
            box.setX(ratio(item.x()));
            box.setY(ratio(item.y()));
            box.setWidth(ratio(item.width()));
            box.setHeight(ratio(item.height()));
            box.setLabel(item.label() == null ? null : normalizeGrade(item.label()));
            box.setConfidence(ratio(item.confidence()));
            return box;
        }).toList();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        return tags.stream().filter(StringUtils::hasText).map(String::trim).map(value -> value.toUpperCase(Locale.ROOT)).distinct().limit(50).toList();
    }

    private String normalizeGrade(String grade) {
        String value = grade == null ? "" : grade.trim().toUpperCase(Locale.ROOT);
        if (!GRADES.contains(value)) throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Unsupported review grade");
        return value;
    }

    private Double ratio(Double value) {
        if (value == null) return null;
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Detection coordinates must be between 0 and 1");
        return value;
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new BusinessException(CommonErrorCode.SYSTEM_ERROR); }
    }

    private AuthenticatedUser operator() { return SecurityContextUtils.currentUser(); }
    private BusinessException taskNotFound() { return new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"); }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
