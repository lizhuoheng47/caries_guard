package com.cariesguard.analysis.interfaces.vo;

import java.util.Map;

/**
 * 可视化资产 VO — 比赛展示增强版.
 * <p>
 * assetTypeLabel 为前端提供中文标签，减少前端翻译逻辑。
 */
public record AnalysisVisualAssetVO(
        String assetTypeCode,
        Long attachmentId,
        Long relatedImageId,
        Long sourceAttachmentId,
        String toothCode,
        Integer sortOrder,
        /** 资产类型中文标签 */
        String assetTypeLabel) {

    private static final Map<String, String> ASSET_TYPE_LABELS = Map.of(
            "HEATMAP", "热力图",
            "MASK", "掩膜图",
            "OVERLAY", "叠加图"
    );

    public AnalysisVisualAssetVO(String assetTypeCode,
                                 Long attachmentId,
                                 Long relatedImageId,
                                 Long sourceAttachmentId,
                                 String toothCode,
                                 Integer sortOrder) {
        this(assetTypeCode, attachmentId, relatedImageId, sourceAttachmentId, toothCode, sortOrder,
                ASSET_TYPE_LABELS.getOrDefault(assetTypeCode != null ? assetTypeCode.toUpperCase(java.util.Locale.ROOT) : "", assetTypeCode));
    }

    public AnalysisVisualAssetVO(String assetTypeCode,
                                 Long attachmentId,
                                 Long relatedImageId,
                                 String toothCode) {
        this(assetTypeCode, attachmentId, relatedImageId, null, toothCode, 0);
    }
}
