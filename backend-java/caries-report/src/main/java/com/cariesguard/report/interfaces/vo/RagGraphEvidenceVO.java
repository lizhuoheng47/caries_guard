package com.cariesguard.report.interfaces.vo;

import java.util.Map;

public record RagGraphEvidenceVO(
        String graphPathId,
        String cypherTemplateCode,
        Double score,
        String evidenceText,
        Map<String, Object> resultPathJson,
        Long chunkId,
        Long docId) {
}
