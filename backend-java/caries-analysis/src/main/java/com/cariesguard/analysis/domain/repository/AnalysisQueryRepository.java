package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import java.util.List;
import java.util.Optional;

public interface AnalysisQueryRepository {

    Optional<AnalysisTaskViewModel> findTask(Long taskId);

    long countTasks(Long caseId, String taskStatusCode);

    List<AnalysisTaskViewModel> pageTasks(Long caseId, String taskStatusCode, int offset, int limit);
}
