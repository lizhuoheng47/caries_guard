package com.cariesguard.analysis.infrastructure.messaging;

import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingAnalysisTaskEventPublisher implements AnalysisTaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAnalysisTaskEventPublisher.class);

    @Override
    public void publishRequested(AnalysisRequestedEvent event) {
        log.info("analysis.requested taskId={} taskNo={} status={}", event.taskId(), event.taskNo(), event.taskStatusCode());
    }

    @Override
    public void publishCompleted(AnalysisCompletedEvent event) {
        log.info("analysis.completed taskId={} taskNo={} caseId={} model={}", event.taskId(), event.taskNo(), event.caseId(), event.modelVersion());
    }

    @Override
    public void publishFailed(AnalysisFailedEvent event) {
        log.info("analysis.failed taskId={} taskNo={} caseId={} error={}", event.taskId(), event.taskNo(), event.caseId(), event.errorMessage());
    }
}
