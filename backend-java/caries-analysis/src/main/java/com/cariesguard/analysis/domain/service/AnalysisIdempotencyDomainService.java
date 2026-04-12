package com.cariesguard.analysis.domain.service;

import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Domain service for analysis task idempotency and retry rules.
 *
 * <p>Retry rules (R-RETRY):
 * <ol>
 *   <li>Only FAILED tasks may be retried. SUCCESS / PROCESSING / QUEUEING are rejected.</li>
 *   <li>Retry creates a new task (new taskId + taskNo), never overwrites the original.</li>
 *   <li>New task links to the original via retryFromTaskId for audit trail.</li>
 * </ol>
 *
 * <p>Late callback rule (R-LATE-CALLBACK):
 * For a FAILED task that has already been retried, any late callback only logs — no business
 * write-back or case status change.
 */
@Component
public class AnalysisIdempotencyDomainService {

    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCESS", "FAILED");

    private final AnaTaskRecordRepository anaTaskRecordRepository;

    public AnalysisIdempotencyDomainService(AnaTaskRecordRepository anaTaskRecordRepository) {
        this.anaTaskRecordRepository = anaTaskRecordRepository;
    }

    /**
     * Check if the incoming callback is a duplicate of an already-processed terminal state.
     * Returns true for same-terminal duplicates (idempotent ack, no further processing).
     */
    public boolean isDuplicateTerminalCallback(AnalysisTaskViewModel task, String incomingStatus) {
        return TERMINAL_STATUSES.contains(task.taskStatusCode()) && task.taskStatusCode().equals(incomingStatus);
    }

    /**
     * Ensure the callback is allowed: reject when task is already in a terminal state with a
     * different terminal status (conflict).
     */
    public void ensureCallbackAllowed(AnalysisTaskViewModel task, String incomingStatus) {
        if (TERMINAL_STATUSES.contains(task.taskStatusCode()) && !task.taskStatusCode().equals(incomingStatus)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(),
                    "Analysis task callback conflicts with terminal status: task=" + task.taskStatusCode() + ", incoming=" + incomingStatus);
        }
    }

    /**
     * Ensure retry is allowed: only FAILED tasks may be retried (R-RETRY rule 1).
     */
    public void ensureRetryAllowed(AnalysisTaskViewModel task) {
        if (!"FAILED".equals(task.taskStatusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(),
                    "Only FAILED tasks may be retried, current status: " + task.taskStatusCode());
        }
    }

    /**
     * Check if a task has already been retried (i.e. a successor task exists with retryFromTaskId = this taskId).
     * Used by R-LATE-CALLBACK: late callbacks on retried tasks are logged only, no business write-back.
     */
    public boolean hasBeenRetried(Long taskId) {
        return anaTaskRecordRepository.existsByRetryFromTaskId(taskId);
    }

    /**
     * Determine if write-back should be skipped for this callback.
     * Skip when: (1) duplicate terminal, or (2) task has already been retried (late callback).
     */
    public boolean shouldSkipWriteBack(AnalysisTaskViewModel task, String incomingStatus) {
        if (isDuplicateTerminalCallback(task, incomingStatus)) {
            return true;
        }
        return hasBeenRetried(task.taskId());
    }
}
