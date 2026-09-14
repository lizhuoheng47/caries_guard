package com.cariesguard.analysis.interfaces.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDraftVO(
        Long draftId,
        Long taskId,
        Integer versionNo,
        String revisedGrade,
        List<ReviewTaskDetailVO.DetectionBoxVO> revisedDetections,
        List<String> reasonTags,
        String note,
        String statusCode,
        Long submittedFeedbackId,
        LocalDateTime updatedAt) {
}
