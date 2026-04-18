package com.cariesguard.analysis.interfaces.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordModelEvaluationCommand(
        @NotNull Long modelVersionId,
        Long datasetSnapshotId,
        @Size(max = 32) String evalTypeCode,
        JsonNode metricJson,
        JsonNode errorCaseJson,
        @Size(max = 500) String evidenceAttachmentKey,
        @Size(max = 500) String remark) {
}
