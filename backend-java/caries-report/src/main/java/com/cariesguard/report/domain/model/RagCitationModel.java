package com.cariesguard.report.domain.model;

public record RagCitationModel(
        Integer rankNo,
        Long docId,
        String docTitle,
        Long chunkId,
        Double score,
        String sourceUri,
        String chunkText) {
}
