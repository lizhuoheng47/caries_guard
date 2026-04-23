package com.cariesguard.analysis.interfaces.vo;

public record CorrectionFeedbackVO(
        Long feedbackId,
        Long caseId,
        String feedbackTypeCode,
        String originalAiGrade,
        String doctorCorrectedGrade,
        Double originalUncertainty,
        Boolean acceptedAiConclusion,
        String correctionReason,
        String exportCandidateFlag,
        String exportedSnapshotNo,
        String trainingCandidateFlag,
        String desensitizedExportFlag,
        String reviewStatusCode,
        String doctorConfirmedGrade,
        Boolean agreedWithAi,
        String correctionReasonCategory,
        String correctionReasonCategoryLabel,
        Boolean agreedWithAiExplanation,
        String followUpSuggestion) {
}
