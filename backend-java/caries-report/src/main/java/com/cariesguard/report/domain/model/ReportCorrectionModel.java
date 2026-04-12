package com.cariesguard.report.domain.model;

import java.time.LocalDateTime;

public record ReportCorrectionModel(
        Long correctionId,
        String feedbackTypeCode,
        String correctedTruthJson,
        LocalDateTime createdAt) {
}

