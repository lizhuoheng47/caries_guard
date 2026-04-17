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
        Integer latencyMs,
        boolean fallback) {
}
