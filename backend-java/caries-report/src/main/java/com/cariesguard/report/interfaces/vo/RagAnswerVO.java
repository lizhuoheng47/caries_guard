package com.cariesguard.report.interfaces.vo;

import java.util.List;

public record RagAnswerVO(
        String sessionNo,
        String requestNo,
        String answerText,
        String answer,
        List<RagCitationVO> citations,
        List<RagRetrievedChunkVO> retrievedChunks,
        List<RagGraphEvidenceVO> graphEvidence,
        String knowledgeBaseCode,
        String knowledgeVersion,
        String modelName,
        String safetyFlag,
        List<String> safetyFlags,
        String refusalReason,
        Double confidence,
        String caseContextSummary,
        String traceId,
        Integer latencyMs,
        boolean fallback) {
}
