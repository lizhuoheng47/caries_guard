package com.cariesguard.dashboard.interfaces.vo;

import java.math.BigDecimal;

public record CorrectionFeedbackStatsVO(
        long totalFeedbackCount,
        long trainingCandidateCount,
        long pendingReviewCount,
        long approvedReviewCount,
        long rejectedReviewCount,
        long exportedSampleCount,
        long aiAcceptedCount,
        long aiCorrectedCount,
        BigDecimal aiCorrectionRate) {
}
