package com.cariesguard.analysis.app;

import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.interfaces.query.AnalysisTaskPageQuery;
import com.cariesguard.analysis.interfaces.vo.AnalysisCitationVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskPageVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisVisualAssetVO;
import com.cariesguard.analysis.interfaces.vo.EvidenceRefItemVO;
import com.cariesguard.analysis.interfaces.vo.ReviewReasonLabels;
import com.cariesguard.analysis.interfaces.vo.RiskLevelLabels;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnalysisQueryAppService {

    private final AnaTaskRecordRepository anaTaskRecordRepository;
    private final AnaResultSummaryRepository anaResultSummaryRepository;
    private final AnaVisualAssetRepository anaVisualAssetRepository;
    private final ObjectMapper objectMapper;

    public AnalysisQueryAppService(AnaTaskRecordRepository anaTaskRecordRepository,
                                   AnaResultSummaryRepository anaResultSummaryRepository,
                                   AnaVisualAssetRepository anaVisualAssetRepository,
                                   ObjectMapper objectMapper) {
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.anaResultSummaryRepository = anaResultSummaryRepository;
        this.anaVisualAssetRepository = anaVisualAssetRepository;
        this.objectMapper = objectMapper;
    }

    public AnalysisTaskDetailVO getTaskDetail(Long taskId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel task = anaTaskRecordRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));
        ensureOrgAccess(operator, task.orgId());
        return toTaskDetailVO(task);
    }

    public AnalysisTaskDetailVO getTaskDetailByTaskNo(String taskNo) {
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "taskNo is required");
        }
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnalysisTaskViewModel task = anaTaskRecordRepository.findByTaskNo(taskNo.trim())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Analysis task does not exist"));
        ensureOrgAccess(operator, task.orgId());
        return toTaskDetailVO(task);
    }

    private AnalysisTaskDetailVO toTaskDetailVO(AnalysisTaskViewModel task) {
        AnalysisSummaryVO summary = anaResultSummaryRepository.findByTaskId(task.taskId()).map(this::toSummaryVO).orElse(null);
        List<AnalysisVisualAssetVO> visualAssets = anaVisualAssetRepository.listByTaskId(task.taskId()).stream()
                .map(item -> new AnalysisVisualAssetVO(
                        item.assetTypeCode(),
                        item.attachmentId(),
                        item.relatedImageId(),
                        item.sourceAttachmentId(),
                        item.toothCode(),
                        item.sortOrder()))
                .toList();
        return new AnalysisTaskDetailVO(
                task.taskId(),
                task.taskNo(),
                task.caseId(),
                task.taskStatusCode(),
                task.taskTypeCode(),
                task.modelVersion(),
                task.errorCode(),
                task.errorMessage(),
                task.createdAt(),
                task.startedAt(),
                task.completedAt(),
                task.traceId(),
                task.inferenceMillis(),
                summary,
                visualAssets);
    }

    public AnalysisTaskPageVO pageTasks(AnalysisTaskPageQuery query) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        Long orgId = operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
        int pageNo = query.pageNo() == null || query.pageNo() < 1 ? 1 : query.pageNo();
        int pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10 : Math.min(query.pageSize(), 100);
        long total = anaTaskRecordRepository.count(query.caseId(), query.taskStatusCode(), orgId);
        int offset = (pageNo - 1) * pageSize;
        List<AnalysisTaskVO> records = anaTaskRecordRepository.pageQuery(query.caseId(), query.taskStatusCode(), orgId, offset, pageSize).stream()
                .map(item -> new AnalysisTaskVO(
                        item.taskId(),
                        item.taskNo(),
                        item.taskStatusCode(),
                        item.taskTypeCode(),
                        item.modelVersion(),
                        item.errorCode(),
                        item.errorMessage(),
                        item.createdAt(),
                        item.startedAt(),
                        item.completedAt(),
                        item.traceId(),
                        item.inferenceMillis()))
                .toList();
        return new AnalysisTaskPageVO(pageNo, pageSize, total, records);
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    // ─── AI 证据展示：核心组装逻辑 ───

    private AnalysisSummaryVO toSummaryVO(AnalysisResultSummaryModel summary) {
        String severity = summary.overallHighestSeverity();
        Double uncertainty = summary.uncertaintyScore() == null ? null : summary.uncertaintyScore().doubleValue();
        String reviewFlag = summary.reviewSuggestedFlag();
        Integer lesionCount = summary.lesionCount();
        Integer abnormalToothCount = summary.abnormalToothCount();
        Integer summaryVersionNo = summary.summaryVersionNo();
        Integer teethCount = null;
        String riskLevel = null;
        String reviewReason = null;
        String doctorReviewRequiredReason = null;
        String knowledgeVersion = null;
        JsonNode riskFactors = null;
        JsonNode evidenceRefs = null;

        // 新增字段
        String gradingLabel = null;
        Double confidenceScore = null;
        Boolean needsReview = null;
        String followUpRecommendation = null;
        JsonNode rawResultJsonNode = null;
        List<AnalysisCitationVO> citations = null;

        if (!StringUtils.hasText(summary.rawResultJson())) {
            return new AnalysisSummaryVO(
                    severity, uncertainty, reviewFlag, lesionCount, abnormalToothCount, summaryVersionNo, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, true, null);
        }
        try {
            JsonNode root = objectMapper.readTree(summary.rawResultJson());
            rawResultJsonNode = root;

            if (!StringUtils.hasText(severity)) {
                severity = textValue(root, "overallHighestSeverity", "overall_highest_severity");
            }
            if (uncertainty == null) {
                uncertainty = doubleValue(root, "uncertaintyScore", "uncertainty_score");
            }
            if (!StringUtils.hasText(reviewFlag)) {
                reviewFlag = textValue(root, "reviewSuggestedFlag", "review_suggested_flag");
            }
            if (lesionCount == null) {
                lesionCount = intValue(root, "lesionCount", "lesion_count");
            }
            if (abnormalToothCount == null) {
                abnormalToothCount = intValue(root, "abnormalToothCount", "abnormal_tooth_count");
            }
            teethCount = intValue(root, "teethCount", "teeth_count");
            riskLevel = textValue(root, "riskLevel", "risk_level");
            reviewReason = textValue(root, "reviewReason", "review_reason");
            doctorReviewRequiredReason = textValue(root, "doctorReviewRequiredReason", "doctor_review_required_reason");
            knowledgeVersion = textValue(root, "knowledgeVersion", "knowledge_version");
            riskFactors = jsonValue(root, "riskFactors", "risk_factors");
            evidenceRefs = jsonValue(root, "evidenceRefs", "evidence_refs");

            // ── 新增字段提取 ──
            gradingLabel = textValue(root, "gradingLabel", "grading_label");
            confidenceScore = doubleValue(root, "confidenceScore", "confidence_score");
            needsReview = booleanValue(root, "needsReview", "needs_review");
            followUpRecommendation = textValue(root, "followUpRecommendation", "follow_up_recommendation",
                    "followupSuggestion", "followup_suggestion");
            citations = extractCitations(root);

            // reviewReason → 人可读标签
            String reviewReasonLabel = ReviewReasonLabels.toLabel(reviewReason);

            // evidenceRefs → 分类展示
            Map<String, List<EvidenceRefItemVO>> classifiedEvidenceRefs = classifyEvidenceRefs(evidenceRefs);

            return new AnalysisSummaryVO(
                    severity,
                    uncertainty,
                    reviewFlag,
                    lesionCount,
                    abnormalToothCount,
                    summaryVersionNo,
                    teethCount,
                    riskLevel,
                    reviewReason,
                    doctorReviewRequiredReason,
                    knowledgeVersion,
                    riskFactors,
                    evidenceRefs,
                    gradingLabel,
                    confidenceScore,
                    needsReview,
                    followUpRecommendation,
                    reviewReasonLabel,
                    classifiedEvidenceRefs,
                    citations,
                    rawResultJsonNode,
                    true,
                    RiskLevelLabels.toLabel(riskLevel));
        } catch (Exception exception) {
            if (StringUtils.hasText(severity) || uncertainty != null || StringUtils.hasText(reviewFlag)) {
                return new AnalysisSummaryVO(
                        severity, uncertainty, reviewFlag, lesionCount, abnormalToothCount, summaryVersionNo, null,
                        riskLevel, reviewReason, doctorReviewRequiredReason, knowledgeVersion, riskFactors, evidenceRefs,
                        gradingLabel, confidenceScore, needsReview, followUpRecommendation,
                        ReviewReasonLabels.toLabel(reviewReason),
                        classifyEvidenceRefs(evidenceRefs), citations, rawResultJsonNode,
                        true, RiskLevelLabels.toLabel(riskLevel));
            }
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "AI summary payload is invalid");
        }
    }

    // ─── evidenceRefs 分类展示 ───

    private Map<String, List<EvidenceRefItemVO>> classifyEvidenceRefs(JsonNode evidenceRefs) {
        if (evidenceRefs == null || !evidenceRefs.isArray() || evidenceRefs.isEmpty()) {
            return null;
        }
        Map<String, List<EvidenceRefItemVO>> classified = new LinkedHashMap<>();
        for (JsonNode ref : evidenceRefs) {
            String refType = textValue(ref, "refType", "ref_type");
            if (refType == null) {
                refType = "OTHER";
            }
            String refCode = textValue(ref, "refCode", "ref_code");
            String refSummary = textValue(ref, "summary");
            String source = textValue(ref, "source");
            classified.computeIfAbsent(refType, k -> new ArrayList<>())
                    .add(new EvidenceRefItemVO(refType, refCode, refSummary, source));
        }
        return classified;
    }

    // ─── citations 提取 ───

    private List<AnalysisCitationVO> extractCitations(JsonNode root) {
        JsonNode citationsNode = jsonValue(root, "citations");
        if (citationsNode == null || !citationsNode.isArray() || citationsNode.isEmpty()) {
            return null;
        }
        List<AnalysisCitationVO> result = new ArrayList<>();
        int rank = 1;
        for (JsonNode c : citationsNode) {
            result.add(new AnalysisCitationVO(
                    intValue(c, "rankNo", "rank_no") != null ? intValue(c, "rankNo", "rank_no") : rank,
                    textValue(c, "docTitle", "doc_title"),
                    textValue(c, "chunkText", "chunk_text"),
                    doubleValue(c, "score"),
                    textValue(c, "sourceUri", "source_uri")));
            rank++;
        }
        return result;
    }

    // ─── JSON 字段提取工具方法 ───

    private String textValue(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private Double doubleValue(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }
        return null;
    }

    private Integer intValue(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isInt()) {
                return value.asInt();
            }
        }
        return null;
    }

    private Boolean booleanValue(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isBoolean()) {
                return value.booleanValue();
            }
        }
        return null;
    }

    private JsonNode jsonValue(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }
}
