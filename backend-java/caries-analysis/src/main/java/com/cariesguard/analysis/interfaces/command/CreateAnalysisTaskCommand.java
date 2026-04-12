package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateAnalysisTaskCommand(
        @NotEmpty List<Long> imageIds,
        String taskTypeCode) {
}
