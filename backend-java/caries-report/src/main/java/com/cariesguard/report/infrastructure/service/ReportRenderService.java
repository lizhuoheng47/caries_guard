package com.cariesguard.report.infrastructure.service;

import com.cariesguard.report.domain.model.ReportRenderDataModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        placeholders.put("toothRecordCount", String.valueOf(renderData.toothRecordCount()));
        placeholders.put("visualAssetCount", String.valueOf(renderData.visualAssetCount()));
        placeholders.put("correctionCount", String.valueOf(renderData.correctionCount()));
        placeholders.put("highestSeverity", value(renderData.highestSeverity()));
        placeholders.put("uncertaintyScore", value(renderData.uncertaintyScore()));
        placeholders.put("lesionCount", value(renderData.lesionCount()));
        placeholders.put("abnormalToothCount", value(renderData.abnormalToothCount()));
        placeholders.put("riskLevelCode", value(renderData.riskLevelCode()));
        placeholders.put("recommendedCycleDays", value(renderData.recommendedCycleDays()));
        placeholders.put("reviewSuggestedFlag", value(renderData.reviewSuggestedFlag()));
        placeholders.put("images", formatImages(renderData.images()));
        placeholders.put("toothFindings", formatToothRecords(renderData.toothRecords()));
        placeholders.put("visualAssets", formatVisualAssets(renderData.visualAssets()));
        placeholders.put("corrections", formatCorrections(renderData.corrections()));
        placeholders.put("clinicalSummary", clinicalSummary(renderData));
        placeholders.put("patientAdvice", patientAdvice(renderData));
        placeholders.put("patientExplanation", patientExplanation(renderData));
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

    private String formatImages(List<ReportImageModel> images) {
        if (CollectionUtils.isEmpty(images)) {
            return "No image records.";
        }
        StringBuilder builder = new StringBuilder();
        for (ReportImageModel image : images) {
            builder.append("- imageId=").append(value(image.imageId()))
                    .append(", type=").append(value(image.imageTypeCode()))
                    .append(", quality=").append(value(image.qualityStatusCode()))
                    .append(", primary=").append(value(image.primaryFlag()))
                    .append(", object=").append(value(image.bucketName())).append('/').append(value(image.objectKey()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatToothRecords(List<ReportToothRecordModel> toothRecords) {
        if (CollectionUtils.isEmpty(toothRecords)) {
            return "No tooth-level findings.";
        }
        StringBuilder builder = new StringBuilder();
        for (ReportToothRecordModel tooth : toothRecords) {
            builder.append("- tooth=").append(value(tooth.toothCode()))
                    .append(", surface=").append(value(tooth.toothSurfaceCode()))
                    .append(", issue=").append(value(tooth.issueTypeCode()))
                    .append(", severity=").append(value(tooth.severityCode()))
                    .append(", finding=").append(value(tooth.findingDesc()))
                    .append(", suggestion=").append(value(tooth.suggestion()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatVisualAssets(List<ReportVisualAssetModel> visualAssets) {
        if (CollectionUtils.isEmpty(visualAssets)) {
            return "No visual assets.";
        }
        StringBuilder builder = new StringBuilder();
        for (ReportVisualAssetModel asset : visualAssets) {
            builder.append("- type=").append(value(asset.assetTypeCode()))
                    .append(", attachmentId=").append(value(asset.attachmentId()))
                    .append(", imageId=").append(value(asset.relatedImageId()))
                    .append(", tooth=").append(value(asset.toothCode()))
                    .append(", object=").append(value(asset.bucketName())).append('/').append(value(asset.objectKey()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatCorrections(List<ReportCorrectionModel> corrections) {
        if (CollectionUtils.isEmpty(corrections)) {
            return "No doctor correction records.";
        }
        StringBuilder builder = new StringBuilder();
        for (ReportCorrectionModel correction : corrections) {
            builder.append("- type=").append(value(correction.feedbackTypeCode()))
                    .append(", corrected=").append(value(correction.correctedTruthJson()))
                    .append(", createdAt=").append(value(correction.createdAt()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String clinicalSummary(ReportRenderDataModel renderData) {
        return "Highest severity: " + value(renderData.highestSeverity())
                + "; uncertainty: " + value(renderData.uncertaintyScore())
                + "; lesion count: " + value(renderData.lesionCount())
                + "; abnormal teeth: " + value(renderData.abnormalToothCount())
                + "; risk: " + value(renderData.riskLevelCode())
                + "; review suggested: " + value(renderData.reviewSuggestedFlag());
    }

    private String patientAdvice(ReportRenderDataModel renderData) {
        String riskLevel = value(renderData.riskLevelCode());
        String cycle = renderData.recommendedCycleDays() == null ? "按医生建议" : renderData.recommendedCycleDays() + " 天左右";
        return "请保持早晚刷牙、减少含糖食物和饮料，并在 " + cycle + " 复查。"
                + " 如出现疼痛、肿胀或牙体缺损，请及时到口腔科就诊。当前风险等级：" + riskLevel + "。";
    }

    private String patientExplanation(ReportRenderDataModel renderData) {
        return renderData.patientExplanation() == null || renderData.patientExplanation().isBlank()
                ? patientAdvice(renderData)
                : renderData.patientExplanation();
    }
}
