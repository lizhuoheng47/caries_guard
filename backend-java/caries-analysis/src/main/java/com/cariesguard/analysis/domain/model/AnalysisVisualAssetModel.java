package com.cariesguard.analysis.domain.model;

public record AnalysisVisualAssetModel(
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder) {

    public AnalysisVisualAssetModel(String assetTypeCode,
                                    Long attachmentId,
                                    Long relatedImageId,
                                    String toothCode) {
        this(assetTypeCode, attachmentId, relatedImageId, null, toothCode, 0);
    }
}
