package com.cariesguard.report.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportRenderDataModel(
        String caseNo,
        Long caseId,
        Long patientId,
        String reportTypeCode,
        List<ReportImageModel> images,
        List<ReportToothRecordModel> toothRecords,
        List<ReportVisualAssetModel> visualAssets,
        String highestSeverity,
        BigDecimal uncertaintyScore,
        Integer lesionCount,
        Integer abnormalToothCount,
        String riskLevelCode,
        Integer recommendedCycleDays,
        String reviewSuggestedFlag,
        List<ReportCorrectionModel> corrections,
        String doctorConclusion,
        String patientExplanation,
        LocalDateTime generatedAt) {

    public int imageCount() {
        return images == null ? 0 : images.size();
    }

    public int toothRecordCount() {
        return toothRecords == null ? 0 : toothRecords.size();
    }

    public int visualAssetCount() {
        return visualAssets == null ? 0 : visualAssets.size();
    }

    public int correctionCount() {
        return corrections == null ? 0 : corrections.size();
    }

    public ReportRenderDataModel withPatientExplanation(String patientExplanation) {
        return new ReportRenderDataModel(
                caseNo,
                caseId,
                patientId,
                reportTypeCode,
                images,
                toothRecords,
                visualAssets,
                highestSeverity,
                uncertaintyScore,
                lesionCount,
                abnormalToothCount,
                riskLevelCode,
                recommendedCycleDays,
                reviewSuggestedFlag,
                corrections,
                doctorConclusion,
                patientExplanation,
                generatedAt);
    }
}
