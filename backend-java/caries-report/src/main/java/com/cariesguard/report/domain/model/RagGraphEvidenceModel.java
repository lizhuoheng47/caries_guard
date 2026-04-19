package com.cariesguard.report.domain.model;

import java.util.Map;

public record RagGraphEvidenceModel(
        String graphPathId,
        String cypherTemplateCode,
        Double score,
        String evidenceText,
        Map<String, Object> resultPathJson,
        Long chunkId,
        Long docId) {
}
