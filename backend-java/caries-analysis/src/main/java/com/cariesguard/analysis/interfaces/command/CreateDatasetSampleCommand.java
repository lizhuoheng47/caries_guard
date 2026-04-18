package com.cariesguard.analysis.interfaces.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDatasetSampleCommand(
        @NotBlank @Size(max = 128) String sampleRefNo,
        @Size(max = 128) String patientUuid,
        @Size(max = 128) String imageRefNo,
        @Size(max = 32) String sourceTypeCode,
        @Size(max = 32) String splitTypeCode,
        @Size(max = 64) String labelVersion,
        JsonNode labelJson) {
}
