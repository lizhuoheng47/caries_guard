package com.cariesguard.analysis.interfaces.vo;

public record AnalysisVisualAssetVO(
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder) {

    public AnalysisVisualAssetVO(String assetTypeCode,
                                 Long attachmentId,
                                 Long relatedImageId,
                                 String toothCode) {
        this(assetTypeCode, attachmentId, relatedImageId, null, toothCode, 0);
    }
}
