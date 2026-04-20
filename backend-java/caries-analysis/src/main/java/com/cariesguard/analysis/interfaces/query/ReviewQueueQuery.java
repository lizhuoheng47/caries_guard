package com.cariesguard.analysis.interfaces.query;

public record ReviewQueueQuery(
        Integer pageNo,
        Integer pageSize,
        String taskStatusCode) {
}

