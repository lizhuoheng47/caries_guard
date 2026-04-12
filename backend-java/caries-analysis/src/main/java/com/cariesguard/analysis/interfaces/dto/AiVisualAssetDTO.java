package com.cariesguard.analysis.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiVisualAssetDTO(
        @NotBlank String assetTypeCode,
        @NotNull Long attachmentId) {
}
