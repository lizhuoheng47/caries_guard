package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.infrastructure.dataobject.AnaReviewDraftDO;
import java.util.Optional;

public interface AnaReviewDraftRepository {
    Optional<AnaReviewDraftDO> findByTaskAndDoctor(Long taskId, Long doctorUserId);

    AnaReviewDraftDO saveDraft(AnaReviewDraftDO draft);

    void markSubmitted(Long draftId, Long feedbackId, Long operatorUserId);
}
