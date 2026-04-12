package com.cariesguard.analysis.domain.model;

public record CorrectionFeedbackCreateModel(
        Long feedbackId,
        Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        Long doctorUserId,
        String originalInferenceJson,
        String correctedTruthJson,
        String feedbackTypeCode,
        String exportedForTrainFlag,
        Long orgId) {
}
