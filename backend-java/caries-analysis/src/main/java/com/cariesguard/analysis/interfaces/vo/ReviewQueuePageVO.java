package com.cariesguard.analysis.interfaces.vo;

import java.util.List;

public record ReviewQueuePageVO(
        Integer pageNo,
        Integer pageSize,
        Long total,
        List<ReviewQueueItemVO> records) {
}

