package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterModelVersionCommand(
        @NotBlank @Size(max = 64) String modelCode,
        @NotBlank @Size(max = 64) String modelVersion,
        @Size(max = 32) String modelTypeCode,
        @Size(max = 500) String remark) {
}
