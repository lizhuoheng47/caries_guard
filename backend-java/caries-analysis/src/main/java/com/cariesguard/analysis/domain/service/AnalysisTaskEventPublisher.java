package com.cariesguard.analysis.domain.service;

import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;

public interface AnalysisTaskEventPublisher {

    void publishRequested(AnalysisRequestedEvent event);
}
