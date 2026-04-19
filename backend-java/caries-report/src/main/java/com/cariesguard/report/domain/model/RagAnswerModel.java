package com.cariesguard.report.domain.model;

import java.util.List;

public record RagAnswerModel(
        String sessionNo,
        String requestNo,
        String answerText,
        List<RagCitationModel> citations,
        List<RagRetrievedChunkModel> retrievedChunks,
        List<RagGraphEvidenceModel> graphEvidence,
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

    public RagAnswerModel {
        citations = citations == null ? List.of() : List.copyOf(citations);
        retrievedChunks = retrievedChunks == null ? List.of() : List.copyOf(retrievedChunks);
        graphEvidence = graphEvidence == null ? List.of() : List.copyOf(graphEvidence);
        safetyFlags = safetyFlags == null ? List.of() : List.copyOf(safetyFlags);
        latencyMs = latencyMs == null ? 0 : latencyMs;
    }

    public RagAnswerModel(String sessionNo,
                          String requestNo,
                          String answerText,
                          List<RagCitationModel> citations,
                          String knowledgeVersion,
                          String modelName,
                          String safetyFlag,
                          Integer latencyMs,
                          boolean fallback) {
        this(sessionNo, requestNo, answerText, citations, List.of(), List.of(), null, knowledgeVersion, modelName, safetyFlag,
                List.of(), null, null, null, null, latencyMs, fallback);
    }

    public RagAnswerModel(String sessionNo,
                          String requestNo,
                          String answerText,
                          List<RagCitationModel> citations,
                          String knowledgeVersion,
                          String modelName,
                          String safetyFlag,
                          List<String> safetyFlags,
                          String refusalReason,
                          Double confidence,
                          String traceId,
                          Integer latencyMs,
                          boolean fallback) {
        this(sessionNo, requestNo, answerText, citations, List.of(), List.of(), null, knowledgeVersion, modelName, safetyFlag,
                safetyFlags, refusalReason, confidence, null, traceId, latencyMs, fallback);
    }

    public static RagAnswerModel fallback(String answerText) {
        return new RagAnswerModel(null, null, answerText, List.of(), List.of(), List.of(), null, null, null, "1",
                List.of("INSUFFICIENT_EVIDENCE"), "RAG_SERVICE_UNAVAILABLE", 0.0, null, null, 0, true);
    }
}
