package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

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
        String trainingCandidateFlag,
        String desensitizedExportFlag,
        String datasetSnapshotNo,
        String reviewStatusCode,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long orgId) {
}
