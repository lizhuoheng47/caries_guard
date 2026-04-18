package com.cariesguard.analysis.interfaces.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateDatasetSnapshotCommand(
        @NotBlank @Size(max = 64) String datasetVersion,
        @Size(max = 32) String snapshotTypeCode,
        @Size(max = 500) String sourceSummary,
        JsonNode metadataJson,
        @Size(max = 500) String datasetCardPath,
        @Size(max = 500) String remark,
        @Valid List<CreateDatasetSampleCommand> samples) {
}
