package com.cariesguard.report.domain.model;

/**
 * RAG 检索片段模型 — 增强版.
 */
public record RagRetrievedChunkModel(
        Long chunkId,
        String documentCode,
        Double score,
        /** 片段文本内容 */
        String chunkText,
        /** 所属文档标题 */
        String docTitle) {

    /** 向后兼容的简化构造器 */
    public RagRetrievedChunkModel(Long chunkId,
                                  String documentCode,
                                  Double score) {
        this(chunkId, documentCode, score, null, null);
    }
}
