package com.cariesguard.followup.domain.repository;

import com.cariesguard.followup.domain.model.FupPlanCreateModel;
import com.cariesguard.followup.domain.model.FupPlanModel;
import java.util.List;
import java.util.Optional;

public interface FupPlanRepository {

    void create(FupPlanCreateModel model);

    Optional<FupPlanModel> findById(Long planId);

    List<FupPlanModel> listByCaseId(Long caseId);

    boolean existsActivePlan(Long caseId, String triggerSourceCode, Long triggerRefId);

    void updateStatus(Long planId, String planStatusCode, Long operatorUserId);
}
