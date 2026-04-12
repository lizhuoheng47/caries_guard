package com.cariesguard.analysis.domain.service;

import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;

public interface AnalysisTaskEventPublisher {

    void publishRequested(AnalysisRequestedEvent event);

    void publishCompleted(AnalysisCompletedEvent event);

    void publishFailed(AnalysisFailedEvent event);
}
