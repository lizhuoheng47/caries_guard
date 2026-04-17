package com.cariesguard.report.interfaces.vo;

public record RagCitationVO(
        Integer rankNo,
        Long docId,
        String docTitle,
        Long chunkId,
        Double score,
        String sourceUri,
        String chunkText) {
}
