package com.cariesguard.analysis.domain.model;

public record AnalysisVisualAssetCreateModel(
        Long assetId,
        Long taskId,
        Long caseId,
        String modelVersion,
        String assetTypeCode,
        Long attachmentId,
        Long orgId,
        Long operatorUserId) {
}
