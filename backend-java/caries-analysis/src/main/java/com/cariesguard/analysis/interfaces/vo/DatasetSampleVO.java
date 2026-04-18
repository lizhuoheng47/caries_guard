package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;

public record DatasetSampleVO(
        Long sampleId,
        Long snapshotId,
        String sampleRefNo,
        String patientUuid,
        String imageRefNo,
        String sourceTypeCode,
        String splitTypeCode,
        String labelVersion,
        JsonNode labelJson) {
}
