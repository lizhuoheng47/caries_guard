package com.cariesguard.analysis.interfaces.query;

public record AnalysisTaskPageQuery(
        Integer pageNo,
        Integer pageSize,
        Long caseId,
        String taskStatusCode) {
}
