package com.cariesguard.analysis.domain.model;

import java.time.LocalDateTime;

public record CorrectionFeedbackExportCandidateModel(
        Long feedbackId,
        Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        Long sourceAttachmentId,
        Long doctorUserId,
        String originalInferenceJson,
        String correctedTruthJson,
        String feedbackTypeCode,
        String reviewStatusCode,
        Long orgId,
        LocalDateTime createdAt) {
}
