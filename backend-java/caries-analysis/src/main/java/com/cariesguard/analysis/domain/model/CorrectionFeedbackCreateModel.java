package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record CorrectionFeedbackCreateModel(
        Long feedbackId,
        Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        Long sourceAttachmentId,
        Long doctorUserId,
        String originalInferenceJson,
        String correctedTruthJson,
        String feedbackTypeCode,
        String exportCandidateFlag,
        String exportedSnapshotNo,
        String trainingCandidateFlag,
        String desensitizedExportFlag,
        String reviewStatusCode,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long orgId) {

    public CorrectionFeedbackCreateModel(Long feedbackId,
                                         Long caseId,
                                         Long diagnosisId,
                                         Long sourceImageId,
                                         Long doctorUserId,
                                         String originalInferenceJson,
                                         String correctedTruthJson,
                                         String feedbackTypeCode,
                                         String legacyExportCandidateFlag,
                                         String trainingCandidateFlag,
                                         String desensitizedExportFlag,
                                         String legacyExportedSnapshotNo,
                                         String reviewStatusCode,
                                         Long reviewedBy,
                                         LocalDateTime reviewedAt,
                                         Long orgId) {
        this(feedbackId, caseId, diagnosisId, sourceImageId, null, doctorUserId, originalInferenceJson,
                correctedTruthJson, feedbackTypeCode, legacyExportCandidateFlag, legacyExportedSnapshotNo,
                trainingCandidateFlag, desensitizedExportFlag, reviewStatusCode, reviewedBy, reviewedAt, orgId);
    }
}
