package com.cariesguard.analysis.domain.service;

import com.cariesguard.analysis.interfaces.command.AiAnalysisResultCallbackCommand;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AnalysisCallbackDomainService {

    private static final Set<String> SUPPORTED_STATUSES = Set.of("PROCESSING", "SUCCESS", "FAILED");

    public String normalizeAndValidateTaskNo(String taskNo) {
        if (!StringUtils.hasText(taskNo)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "taskNo is required");
        }
        return taskNo.trim();
    }

    public String normalizeAndValidateTaskStatus(String taskStatusCode) {
        if (!StringUtils.hasText(taskStatusCode)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "taskStatusCode is required");
        }
        String normalized = taskStatusCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATUSES.contains(normalized)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Unsupported analysis callback status");
        }
        return normalized;
    }

    public void validateSuccessCallbackCompleteness(AiAnalysisResultCallbackCommand callback) {
        boolean hasSummary = callback.summary() != null;
        boolean hasRawResult = callback.rawResultJson() != null && !callback.rawResultJson().isNull();
        if (!hasSummary && !hasRawResult) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "AI success callback must contain summary or rawResultJson");
        }
    }

    public String resolveTargetCaseStatus(String taskStatusCode) {
        return switch (taskStatusCode) {
            case "SUCCESS" -> "REVIEW_PENDING";
            case "FAILED" -> "QC_PENDING";
            default -> throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Cannot resolve case status for task status: " + taskStatusCode);
        };
    }

    public String resolveChangeReasonCode(String taskStatusCode) {
        return switch (taskStatusCode) {
            case "SUCCESS" -> "AI_CALLBACK_SUCCESS";
            case "FAILED" -> "AI_CALLBACK_FAILED";
            default -> "AI_CALLBACK_" + taskStatusCode;
        };
    }

    public SummaryAggregates extractSummaryAggregates(AiAnalysisResultCallbackCommand.Summary summary,
                                                      JsonNode rawResultJson,
                                                      Double topLevelUncertaintyScore) {
        String severity = null;
        BigDecimal uncertainty = null;
        String reviewFlag = "0";

        if (summary != null) {
            severity = summary.overallHighestSeverity();
            uncertainty = summary.uncertaintyScore() != null ? BigDecimal.valueOf(summary.uncertaintyScore()) : null;
            reviewFlag = summary.reviewSuggestedFlag() != null ? summary.reviewSuggestedFlag() : "0";
        } else if (rawResultJson != null && !rawResultJson.isNull()) {
            severity = textValueFromJson(rawResultJson, "overallHighestSeverity", "overall_highest_severity");
            Double uncertaintyDouble = doubleValueFromJson(rawResultJson, "uncertaintyScore", "uncertainty_score");
            uncertainty = uncertaintyDouble != null ? BigDecimal.valueOf(uncertaintyDouble) : null;
            String flag = textValueFromJson(rawResultJson, "reviewSuggestedFlag", "review_suggested_flag");
            reviewFlag = flag != null ? flag : "0";
        }
        if (uncertainty == null && topLevelUncertaintyScore != null) {
            uncertainty = BigDecimal.valueOf(topLevelUncertaintyScore);
        }

        return new SummaryAggregates(severity, uncertainty, reviewFlag);
    }

    public record SummaryAggregates(
            String overallHighestSeverity,
            BigDecimal uncertaintyScore,
            String reviewSuggestedFlag) {
    }

    private String textValueFromJson(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull() && node.isTextual()) {
                return node.asText();
            }
        }
        return null;
    }

    private Double doubleValueFromJson(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node != null && node.isNumber()) {
                return node.asDouble();
            }
        }
        return null;
    }
}
