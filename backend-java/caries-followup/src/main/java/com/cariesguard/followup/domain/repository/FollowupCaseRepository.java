package com.cariesguard.followup.domain.repository;

import com.cariesguard.followup.domain.model.FollowupCaseModel;
import java.util.Optional;

public interface FollowupCaseRepository {

    Optional<FollowupCaseModel> findCase(Long caseId);
}
