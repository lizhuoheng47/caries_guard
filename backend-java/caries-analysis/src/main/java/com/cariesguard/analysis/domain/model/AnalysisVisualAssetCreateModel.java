package com.cariesguard.analysis.domain.model;

public record AnalysisVisualAssetCreateModel(
        Long assetId,
        Long taskId,
        Long caseId,
        String modelVersion,
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder,
        Long orgId,
        Long operatorUserId) {

    public AnalysisVisualAssetCreateModel(Long assetId,
                                          Long taskId,
                                          Long caseId,
                                          String modelVersion,
                                          String assetTypeCode,
                                          Long attachmentId,
                                          Long relatedImageId,
                                          String toothCode,
                                          Long orgId,
                                          Long operatorUserId) {
        this(assetId, taskId, caseId, modelVersion, assetTypeCode, attachmentId, relatedImageId,
                null, toothCode, 0, orgId, operatorUserId);
    }
}
