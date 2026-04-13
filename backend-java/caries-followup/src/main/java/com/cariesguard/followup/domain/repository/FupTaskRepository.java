package com.cariesguard.followup.domain.repository;

import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FupTaskRepository {

    void create(FupTaskCreateModel model);

    Optional<FupTaskModel> findById(Long taskId);

    List<FupTaskModel> listByPlanId(Long planId);

    List<FupTaskModel> listByCaseId(Long caseId);

    void updateStatus(Long taskId, String taskStatusCode, LocalDateTime completedAt, Long operatorUserId);

    void assignTask(Long taskId, Long assignedToUserId, Long operatorUserId);

    boolean allTasksDoneOrCancelled(Long planId);
}
