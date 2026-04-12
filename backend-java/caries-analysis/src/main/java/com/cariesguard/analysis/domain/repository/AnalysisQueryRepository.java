package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import java.util.Optional;

public interface AnalysisQueryRepository {

    Optional<AnalysisTaskViewModel> findTask(Long taskId);
}
