package com.cariesguard.analysis.interfaces.vo;

import java.util.Locale;
import java.util.Map;

public record AnalysisVisualAssetVO(
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder,
        String accessUrl,
        String assetTypeLabel) {

    private static final Map<String, String> ASSET_TYPE_LABELS = Map.of(
            "HEATMAP", "Heatmap",
            "MASK", "Mask",
            "OVERLAY", "Overlay"
    );

    public AnalysisVisualAssetVO(String assetTypeCode,
                                 Long attachmentId,
                                 Long relatedImageId,
                                 Long sourceAttachmentId,
                                 String toothCode,
                                 Integer sortOrder,
                                 String accessUrl) {
        this(
                assetTypeCode,
                attachmentId,
                relatedImageId,
                sourceAttachmentId,
                toothCode,
                sortOrder,
                accessUrl,
                ASSET_TYPE_LABELS.getOrDefault(
                        assetTypeCode != null ? assetTypeCode.toUpperCase(Locale.ROOT) : "",
                        assetTypeCode)
        );
    }

    public AnalysisVisualAssetVO(String assetTypeCode,
                                 Long attachmentId,
                                 Long relatedImageId,
                                 String toothCode) {
        this(assetTypeCode, attachmentId, relatedImageId, null, toothCode, 0, null);
    }
}
