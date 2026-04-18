package com.cariesguard.report.domain.model;

public record RagRetrievedChunkModel(
        Long chunkId,
        String documentCode,
        Double score) {
}
