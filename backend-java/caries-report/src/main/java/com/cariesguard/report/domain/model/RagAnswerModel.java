package com.cariesguard.report.domain.model;

import java.util.List;

public record RagAnswerModel(
        String sessionNo,
        String requestNo,
        String answerText,
        List<RagCitationModel> citations,
        String knowledgeVersion,
        String modelName,
        String safetyFlag,
        Integer latencyMs,
        boolean fallback) {

    public RagAnswerModel {
        citations = citations == null ? List.of() : List.copyOf(citations);
        latencyMs = latencyMs == null ? 0 : latencyMs;
    }

    public static RagAnswerModel fallback(String answerText) {
        return new RagAnswerModel(null, null, answerText, List.of(), null, null, "1", 0, true);
    }
}
