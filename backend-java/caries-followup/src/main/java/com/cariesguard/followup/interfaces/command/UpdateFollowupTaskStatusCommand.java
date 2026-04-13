package com.cariesguard.followup.interfaces.command;

import jakarta.validation.constraints.NotBlank;

public record UpdateFollowupTaskStatusCommand(
        @NotBlank String targetStatusCode,
        String remark) {
}
