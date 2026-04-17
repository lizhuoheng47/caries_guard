package com.cariesguard.report.interfaces.vo;

import java.time.LocalDateTime;

public record ReportCorrectionVO(
        Long correctionId,
        String feedbackTypeCode,
        String correctedTruthJson,
        LocalDateTime createdAt) {
}
