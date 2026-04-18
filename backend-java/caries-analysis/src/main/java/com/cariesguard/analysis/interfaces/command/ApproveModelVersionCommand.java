package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveModelVersionCommand(
        @NotBlank @Size(max = 32) String decisionCode,
        @Size(max = 500) String remark) {
}
