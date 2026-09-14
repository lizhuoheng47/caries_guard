package com.cariesguard.analysis.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveReviewDraftCommand(
        @NotBlank @Size(max = 16) String revisedGrade,
        @Size(max = 200) List<DetectionBox> revisedDetections,
        @Size(max = 50) List<@Size(max = 64) String> reasonTags,
        @Size(max = 2000) String note) {

    public record DetectionBox(
            @Size(max = 64) String id,
            Double x,
            Double y,
            Double width,
            Double height,
            @Size(max = 16) String label,
            Double confidence) {
    }
}
