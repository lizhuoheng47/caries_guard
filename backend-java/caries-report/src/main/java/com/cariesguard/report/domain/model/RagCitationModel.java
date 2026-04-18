package com.cariesguard.report.domain.model;

public record RagCitationModel(
        Integer rankNo,
        String knowledgeBaseCode,
        String documentCode,
        String documentVersion,
        Long docId,
        String docTitle,
        Long chunkId,
        Double score,
        Double retrievalScore,
        String sourceUri,
        String chunkText) {

    public RagCitationModel(Integer rankNo,
                            Long docId,
                            String docTitle,
                            Long chunkId,
                            Double score,
                            String sourceUri,
                            String chunkText) {
        this(rankNo, null, null, null, docId, docTitle, chunkId, score, score, sourceUri, chunkText);
    }
}
