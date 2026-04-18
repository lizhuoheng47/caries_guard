package com.cariesguard.analysis.interfaces.vo;

public record CorrectionFeedbackReviewVO(
        Integer reviewedCount,
        String reviewStatusCode,
        String trainingCandidateFlag) {
}
