package com.cariesguard.analysis.interfaces.vo;

/**
 * 分析结果中的知识库引用 citation.
 */
public record AnalysisCitationVO(
        /** 引用排序 */
        Integer rankNo,
        /** 文档标题 */
        String docTitle,
        /** 片段文本 */
        String chunkText,
        /** 相关度分数 */
        Double score,
        /** 来源 URI */
        String sourceUri) {
}
