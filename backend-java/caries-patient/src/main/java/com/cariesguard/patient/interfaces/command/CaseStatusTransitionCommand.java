package com.cariesguard.patient.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record CaseStatusTransitionCommand(
        @NotBlank String targetStatusCode,
        @NotBlank String reasonCode,
        String reasonRemark) {
}
