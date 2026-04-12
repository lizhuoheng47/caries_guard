package com.cariesguard.image.interfaces.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateCaseImageCommand(
        @NotNull Long attachmentId,
        @NotNull Long visitId,
        @NotNull Long patientId,
        String imageTypeCode,
        String imageSourceCode,
        LocalDateTime shootingTime,
        String bodyPositionCode,
        String primaryFlag,
        String remark) {
}
