package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import java.util.Optional;

public interface AnaResultSummaryRepository {

    void save(AnalysisResultSummaryModel model);

    Optional<AnalysisResultSummaryModel> findByTaskId(Long taskId);

    Optional<AnalysisResultSummaryModel> findLatestByCaseId(Long caseId);
}
