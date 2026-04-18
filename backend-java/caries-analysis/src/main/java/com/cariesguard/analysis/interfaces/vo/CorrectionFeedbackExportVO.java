package com.cariesguard.analysis.interfaces.vo;

import java.util.List;

public record CorrectionFeedbackExportVO(
        String snapshotNo,
        Integer sampleCount,
        List<CorrectionFeedbackExportSampleVO> samples) {
}
