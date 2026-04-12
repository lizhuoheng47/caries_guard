package com.cariesguard.patient.domain.model;

import java.util.List;

public record PageQueryResult<T>(
        List<T> records,
        long total,
        int pageNo,
        int pageSize) {
}
