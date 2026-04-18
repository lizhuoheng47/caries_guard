package com.cariesguard.report.interfaces.vo;

public record RagRetrievedChunkVO(
        Long chunkId,
        String documentCode,
        Double score) {
}
