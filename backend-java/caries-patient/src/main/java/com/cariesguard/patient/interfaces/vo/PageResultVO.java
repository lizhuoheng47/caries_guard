package com.cariesguard.patient.interfaces.vo;

import java.util.List;

public record PageResultVO<T>(
        List<T> records,
        long total,
        int pageNo,
        int pageSize) {
}
