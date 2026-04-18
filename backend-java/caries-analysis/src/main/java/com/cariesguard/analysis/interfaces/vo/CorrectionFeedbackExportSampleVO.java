package com.cariesguard.analysis.interfaces.vo;

public record CorrectionFeedbackExportSampleVO(
        Long feedbackId,
        Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        Long sourceAttachmentId,
        String feedbackTypeCode,
        String reviewStatusCode,
        String originalAiGrade,
        String doctorCorrectedGrade,
        Double originalUncertainty,
        Boolean acceptedAiConclusion,
        String correctionReason,
        String governanceSchemaVersion) {
}
