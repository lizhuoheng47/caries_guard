package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record DatasetSnapshotVO(
        Long snapshotId,
        String datasetVersion,
        String snapshotTypeCode,
        String sourceSummary,
        Integer sampleCount,
        JsonNode metadataJson,
        String datasetCardPath,
        List<DatasetSampleVO> samples) {
}
