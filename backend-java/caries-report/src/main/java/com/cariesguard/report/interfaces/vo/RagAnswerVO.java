package com.cariesguard.report.interfaces.vo;

import java.util.List;

public record RagAnswerVO(
        String sessionNo,
        String requestNo,
        String answerText,
        List<RagCitationVO> citations,
        String knowledgeVersion,
        String modelName,
        String safetyFlag,
        List<String> safetyFlags,
        String refusalReason,
        Double confidence,
        String traceId,
        Integer latencyMs,
        boolean fallback) {
}
