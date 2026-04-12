package com.cariesguard.report.infrastructure.service;

import com.cariesguard.report.domain.model.ReportRenderDataModel;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReportRenderService {

    public String render(String templateContent, String reportNo, ReportRenderDataModel renderData) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("reportNo", value(reportNo));
        placeholders.put("caseNo", value(renderData.caseNo()));
        placeholders.put("caseId", value(renderData.caseId()));
        placeholders.put("patientId", value(renderData.patientId()));
        placeholders.put("reportTypeCode", value(renderData.reportTypeCode()));
        placeholders.put("imageCount", String.valueOf(renderData.imageCount()));
        placeholders.put("highestSeverity", value(renderData.highestSeverity()));
        placeholders.put("uncertaintyScore", value(renderData.uncertaintyScore()));
        placeholders.put("riskLevelCode", value(renderData.riskLevelCode()));
        placeholders.put("recommendedCycleDays", value(renderData.recommendedCycleDays()));
        placeholders.put("reviewSuggestedFlag", value(renderData.reviewSuggestedFlag()));
        placeholders.put("latestCorrectionJson", value(renderData.latestCorrectionJson()));
        placeholders.put("doctorConclusion", value(renderData.doctorConclusion()));
        placeholders.put("generatedAt", value(renderData.generatedAt()));

        String rendered = templateContent;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

