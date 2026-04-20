package com.cariesguard.analysis.assembler;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.ReviewQueueItemVO;
import com.cariesguard.analysis.interfaces.vo.ReviewReasonLabels;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReviewQueueAssembler {

    public ReviewQueueItemVO toItem(AnalysisTaskDetailVO task, AnalysisCaseModel medicalCase) {
        AnalysisSummaryVO summary = task.summary();
        String reviewReason = summary == null ? null : summary.reviewReason();
        List<String> reviewReasonCodes = splitCodes(reviewReason);
        List<String> reviewReasonLabels = reviewReasonCodes.stream()
                .map(ReviewReasonLabels::toLabel)
                .toList();
        Boolean needsReview = resolveNeedsReview(summary);
        return new ReviewQueueItemVO(
                task.taskNo(),
                task.caseId(),
                medicalCase == null ? null : medicalCase.caseNo(),
                task.taskStatusCode(),
                needsReview ? "PENDING" : "NOT_REQUIRED",
                null,
                summary == null ? null : summary.gradingLabel(),
                summary == null ? null : summary.uncertaintyScore(),
                needsReview,
                summary == null ? null : summary.riskLevel(),
                reviewReasonCodes,
                reviewReasonLabels,
                firstToothCode(summary),
                task.createdAt(),
                task.completedAt());
    }

    private Boolean resolveNeedsReview(AnalysisSummaryVO summary) {
        if (summary == null) {
            return Boolean.FALSE;
        }
        if (summary.needsReview() != null) {
            return summary.needsReview();
        }
        return "1".equals(summary.reviewSuggestedFlag());
    }

    private List<String> splitCodes(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> item.toUpperCase(Locale.ROOT))
                .toList();
    }

    private String firstToothCode(AnalysisSummaryVO summary) {
        if (summary == null || summary.rawResultJson() == null) {
            return null;
        }
        JsonNode lesionResults = summary.rawResultJson().get("lesionResults");
        if (lesionResults == null || !lesionResults.isArray() || lesionResults.isEmpty()) {
            return null;
        }
        JsonNode first = lesionResults.get(0);
        JsonNode toothCode = first.get("toothCode");
        return toothCode == null || toothCode.isNull() ? null : toothCode.asText(null);
    }
}

