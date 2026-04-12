package com.cariesguard.analysis.interfaces.vo;

import java.util.List;

public record AnalysisTaskPageVO(
        Integer pageNo,
        Integer pageSize,
        Long total,
        List<AnalysisTaskVO> records) {
}
