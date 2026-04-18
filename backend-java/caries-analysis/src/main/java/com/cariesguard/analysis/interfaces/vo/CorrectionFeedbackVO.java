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
        String reviewStatusCode) {

    public CorrectionFeedbackVO(Long feedbackId,
                                Long caseId,
                                String feedbackTypeCode,
                                String exportCandidateFlag,
                                String exportedSnapshotNo,
                                String trainingCandidateFlag,
                                String desensitizedExportFlag,
                                String reviewStatusCode) {
        this(feedbackId, caseId, feedbackTypeCode, null, null, null, null, null,
                exportCandidateFlag, exportedSnapshotNo, trainingCandidateFlag, desensitizedExportFlag, reviewStatusCode);
    }
}
