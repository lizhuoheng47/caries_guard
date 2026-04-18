package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackExportCandidateModel;
import java.time.LocalDateTime;
import java.util.List;

public interface AnaCorrectionFeedbackRepository {

    void save(CorrectionFeedbackCreateModel model);

    List<CorrectionFeedbackExportCandidateModel> listTrainingCandidates(Long orgId, int limit);

    void markExported(List<Long> feedbackIds, String snapshotNo);

    int reviewFeedbacks(List<Long> feedbackIds,
                        String reviewStatusCode,
                        String trainingCandidateFlag,
                        Long reviewerUserId,
                        LocalDateTime reviewedAt,
                        Long orgId);
}
