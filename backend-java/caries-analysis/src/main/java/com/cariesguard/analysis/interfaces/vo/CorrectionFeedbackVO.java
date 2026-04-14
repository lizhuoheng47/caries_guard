package com.cariesguard.analysis.interfaces.vo;

public record CorrectionFeedbackVO(
        Long feedbackId,
        Long caseId,
        String feedbackTypeCode,
        String exportedForTrainFlag,
        String trainingCandidateFlag,
        String desensitizedExportFlag,
        String datasetSnapshotNo,
        String reviewStatusCode) {
}
