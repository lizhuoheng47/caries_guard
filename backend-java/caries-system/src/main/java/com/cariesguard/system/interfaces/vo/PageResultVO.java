package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record PageResultVO<T>(
        List<T> records,
        long total,
        int pageNo,
        int pageSize) {
}
