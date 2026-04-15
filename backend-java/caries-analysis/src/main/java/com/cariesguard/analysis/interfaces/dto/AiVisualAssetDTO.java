package com.cariesguard.analysis.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record AiVisualAssetDTO(
        @NotBlank String assetTypeCode,
        Long attachmentId,
        String bucketName,
        String objectKey,
        String contentType,
        Long relatedImageId,
        String toothCode,
        Long fileSizeBytes,
        String md5) {
}
