package com.cariesguard.analysis.interfaces.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 提交纠正反馈命令 — 比赛展示增强版.
 * <p>
 * 新增字段支持医生结构化复核：确认分级、是否同意 AI、修正原因分类、
 * 是否同意智能解释、复查建议。
 */
public record SubmitCorrectionFeedbackCommand(
        @NotNull Long caseId,
        Long diagnosisId,
        Long sourceImageId,
        @NotBlank String feedbackTypeCode,
        JsonNode originalInferenceJson,
        JsonNode correctedTruthJson,
        String originalAiGrade,
        String doctorCorrectedGrade,
        Double originalUncertainty,
        Boolean acceptedAiConclusion,
        String correctionReason,
        Boolean trainingCandidate,

        /* ── 新增：结构化复核字段 ── */
        /** 医生确认分级（医生最终判定的龋齿等级） */
        String doctorConfirmedGrade,
        /** 是否同意 AI 分级结论 */
        Boolean agreedWithAi,
        /** 修正原因分类编码，如 OVER_GRADED / UNDER_GRADED / WRONG_TOOTH / MISSED_LESION */
        String correctionReasonCategory,
        /** 是否同意 AI 生成的智能解释 */
        Boolean agreedWithAiExplanation,
        /** 医生给出的复查建议 */
        String followUpSuggestion) {

    /** 向后兼容的简化构造器 */
    public SubmitCorrectionFeedbackCommand(Long caseId,
                                           Long diagnosisId,
                                           Long sourceImageId,
                                           String feedbackTypeCode,
                                           JsonNode originalInferenceJson,
                                           JsonNode correctedTruthJson) {
        this(caseId, diagnosisId, sourceImageId, feedbackTypeCode, originalInferenceJson, correctedTruthJson,
                null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
