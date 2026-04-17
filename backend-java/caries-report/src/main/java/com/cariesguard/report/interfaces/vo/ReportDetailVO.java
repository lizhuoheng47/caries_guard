package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ReportDetailVO(
        Long reportId,
        String reportNo,
        Long caseId,
        String caseNo,
        Long patientId,
        Long attachmentId,
        Long sourceSummaryId,
        Long sourceRiskAssessmentId,
        Long sourceCorrectionId,
        String reportTypeCode,
        String reportStatusCode,
        Integer versionNo,
        String summaryText,
        ReportAnalysisSummaryVO analysisSummary,
        ReportRiskAssessmentVO riskAssessment,
        List<ReportImageVO> images,
        List<ReportToothRecordVO> toothRecords,
        List<ReportVisualAssetVO> visualAssets,
        List<ReportCorrectionVO> corrections,
        LocalDateTime generatedAt,
        LocalDateTime signedAt,
        LocalDateTime createdAt) {
}
