package com.cariesguard.analysis.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.common.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisIdempotencyDomainServiceTests {

    @Mock
    private AnaTaskRecordRepository anaTaskRecordRepository;

    @InjectMocks
    private AnalysisIdempotencyDomainService service;

    @Test
    void isDuplicateShouldReturnTrueForSameTerminal() {
        AnalysisTaskViewModel task = taskWithStatus("SUCCESS");
        assertThat(service.isDuplicateTerminalCallback(task, "SUCCESS")).isTrue();
    }

    @Test
    void isDuplicateShouldReturnFalseForNonTerminal() {
        AnalysisTaskViewModel task = taskWithStatus("PROCESSING");
        assertThat(service.isDuplicateTerminalCallback(task, "SUCCESS")).isFalse();
    }

    @Test
    void ensureCallbackAllowedShouldThrowForConflictTerminal() {
        AnalysisTaskViewModel task = taskWithStatus("SUCCESS");
        assertThatThrownBy(() -> service.ensureCallbackAllowed(task, "FAILED"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ensureRetryAllowedShouldPassForFailedTask() {
        AnalysisTaskViewModel task = taskWithStatus("FAILED");
        service.ensureRetryAllowed(task); // no exception
    }

    @Test
    void ensureRetryAllowedShouldThrowForSuccessTask() {
        AnalysisTaskViewModel task = taskWithStatus("SUCCESS");
        assertThatThrownBy(() -> service.ensureRetryAllowed(task))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void hasBeenRetriedShouldDelegateToRepository() {
        when(anaTaskRecordRepository.existsByRetryFromTaskId(6001L)).thenReturn(true);
        assertThat(service.hasBeenRetried(6001L)).isTrue();
    }

    @Test
    void shouldSkipWriteBackForDuplicateTerminal() {
        AnalysisTaskViewModel task = taskWithStatus("SUCCESS");
        assertThat(service.shouldSkipWriteBack(task, "SUCCESS")).isTrue();
    }

    @Test
    void shouldSkipWriteBackForRetriedTask() {
        AnalysisTaskViewModel task = taskWithStatus("QUEUEING");
        when(anaTaskRecordRepository.existsByRetryFromTaskId(task.taskId())).thenReturn(true);
        assertThat(service.shouldSkipWriteBack(task, "SUCCESS")).isTrue();
    }

    private AnalysisTaskViewModel taskWithStatus(String status) {
        return new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", status, null,
                LocalDateTime.now(), null, null, 100001L, null);
    }
}
