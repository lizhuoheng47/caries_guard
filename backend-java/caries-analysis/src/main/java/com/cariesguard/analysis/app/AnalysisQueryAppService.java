package com.cariesguard.analysis.app;

import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.interfaces.query.AnalysisTaskPageQuery;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskPageVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisVisualAssetVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
        AnalysisSummaryVO summary = anaResultSummaryRepository.findByTaskId(taskId).map(this::toSummaryVO).orElse(null);
        List<AnalysisVisualAssetVO> visualAssets = anaVisualAssetRepository.listByTaskId(taskId).stream()
                .map(item -> new AnalysisVisualAssetVO(item.assetTypeCode(), item.attachmentId(), item.relatedImageId(), item.toothCode()))
                .toList();
        return new AnalysisTaskDetailVO(
                task.taskId(),
                task.taskNo(),
                task.caseId(),
                task.taskStatusCode(),
                task.taskTypeCode(),
                task.modelVersion(),
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

    private AnalysisSummaryVO toSummaryVO(AnalysisResultSummaryModel summary) {
        String severity = summary.overallHighestSeverity();
        Double uncertainty = summary.uncertaintyScore() == null ? null : summary.uncertaintyScore().doubleValue();
        String reviewFlag = summary.reviewSuggestedFlag();
        Integer teethCount = null;
        if (!StringUtils.hasText(summary.rawResultJson())) {
            return new AnalysisSummaryVO(severity, uncertainty, reviewFlag, null);
        }
        try {
            JsonNode root = objectMapper.readTree(summary.rawResultJson());
            if (!StringUtils.hasText(severity)) {
                severity = textValue(root, "overallHighestSeverity", "overall_highest_severity");
            }
            if (uncertainty == null) {
                uncertainty = doubleValue(root, "uncertaintyScore", "uncertainty_score");
            }
            if (!StringUtils.hasText(reviewFlag)) {
                reviewFlag = textValue(root, "reviewSuggestedFlag", "review_suggested_flag");
            }
            teethCount = intValue(root, "teethCount", "teeth_count");
            return new AnalysisSummaryVO(
                    severity,
                    uncertainty,
                    reviewFlag,
                    teethCount);
        } catch (Exception exception) {
            if (StringUtils.hasText(severity) || uncertainty != null || StringUtils.hasText(reviewFlag)) {
                return new AnalysisSummaryVO(severity, uncertainty, reviewFlag, null);
            }
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "AI summary payload is invalid");
        }
    }

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
}



