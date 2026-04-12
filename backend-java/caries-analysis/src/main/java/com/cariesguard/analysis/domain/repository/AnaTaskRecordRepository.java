package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskStatusUpdateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import java.util.List;
import java.util.Optional;

public interface AnaTaskRecordRepository {

    void save(AnalysisTaskCreateModel model);

    Optional<AnalysisTaskViewModel> findById(Long taskId);

    Optional<AnalysisTaskViewModel> findByTaskNo(String taskNo);

    boolean existsRunningTaskByCaseId(Long caseId);

    void updateStatus(AnalysisTaskStatusUpdateModel model);

    long count(Long caseId, String taskStatusCode, Long orgId);

    List<AnalysisTaskViewModel> pageQuery(Long caseId, String taskStatusCode, Long orgId, int offset, int limit);

    /**
     * Check if a successor retry task exists for the given original task ID.
     * Used by R-LATE-CALLBACK rule: late callbacks on retried tasks skip business write-back.
     */
    boolean existsByRetryFromTaskId(Long originalTaskId);
}
