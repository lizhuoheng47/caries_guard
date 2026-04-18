package com.cariesguard.analysis.interfaces.vo;

/**
 * 纠正反馈 VO — 比赛展示增强版.
 * <p>
 * 新增结构化复核字段，使比赛讲解时可以展示医生与 AI 的交互闭环。
 */
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

        /* ── 新增：结构化复核字段 ── */
        /** 医生确认分级 */
        String doctorConfirmedGrade,
        /** 是否同意 AI 分级 */
        Boolean agreedWithAi,
        /** 修正原因分类编码 */
        String correctionReasonCategory,
        /** 修正原因分类的人可读标签 */
        String correctionReasonCategoryLabel,
        /** 是否同意 AI 智能解释 */
        Boolean agreedWithAiExplanation,
        /** 医生复查建议 */
        String followUpSuggestion) {

    /** 向后兼容的简化构造器 */
    public CorrectionFeedbackVO(Long feedbackId,
                                Long caseId,
                                String feedbackTypeCode,
                                String exportCandidateFlag,
                                String exportedSnapshotNo,
                                String trainingCandidateFlag,
                                String desensitizedExportFlag,
                                String reviewStatusCode) {
        this(feedbackId, caseId, feedbackTypeCode, null, null, null, null, null,
                exportCandidateFlag, exportedSnapshotNo, trainingCandidateFlag, desensitizedExportFlag, reviewStatusCode,
                null, null, null, null, null, null);
    }
}
