package com.cariesguard.analysis.interfaces.vo;

public record AnalysisCallbackAckVO(
        String taskNo,
        String taskStatusCode,
        boolean idempotent) {
}
