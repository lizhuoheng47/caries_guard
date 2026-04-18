package com.cariesguard.report.interfaces.vo;

/**
 * RAG 检索片段 VO — 比赛展示增强版.
 * <p>
 * 增加了片段文本和文档标题，使比赛讲解时可以直观展示
 * RAG 检索到了哪些知识库内容。
 */
public record RagRetrievedChunkVO(
        Long chunkId,
        String documentCode,
        Double score,
        /** 片段文本内容（用于比赛展示） */
        String chunkText,
        /** 所属文档标题 */
        String docTitle) {

    /** 向后兼容的简化构造器 */
    public RagRetrievedChunkVO(Long chunkId,
                               String documentCode,
                               Double score) {
        this(chunkId, documentCode, score, null, null);
    }
}
