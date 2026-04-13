package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AnalysisCallbackIdempotencyE2ETest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldKeepWriteBackIdempotentForDuplicateSuccessCallback() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "idempotent"));

        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String firstTimestamp = fixture.currentEpochSecond();
        String firstSignature = fixture.signCallback(callbackBody, firstTimestamp);
        AnalysisCallbackAckVO firstAck = fixture.analysisCallbackAppService.handleResultCallback(callbackBody, firstTimestamp, firstSignature);
        assertThat(firstAck.idempotent()).isFalse();

        int summaryCountAfterFirst = fixture.state.summariesByTaskId.size();
        int visualCountAfterFirst = fixture.visualAssetRepository.listByTaskId(task.taskId()).size();
        int riskCountAfterFirst = fixture.riskRepository.records().size();
        int statusLogCountAfterFirst = fixture.state.caseStatusLogs.size();

        String secondTimestamp = fixture.currentEpochSecond();
        String secondSignature = fixture.signCallback(callbackBody, secondTimestamp);
        AnalysisCallbackAckVO secondAck = fixture.analysisCallbackAppService.handleResultCallback(callbackBody, secondTimestamp, secondSignature);

        assertThat(secondAck.taskStatusCode()).isEqualTo("SUCCESS");
        assertThat(secondAck.idempotent()).isTrue();
        assertThat(fixture.state.summariesByTaskId).hasSize(summaryCountAfterFirst);
        assertThat(fixture.visualAssetRepository.listByTaskId(task.taskId())).hasSize(visualCountAfterFirst);
        assertThat(fixture.riskRepository.records()).hasSize(riskCountAfterFirst);
        assertThat(fixture.state.caseStatusLogs).hasSize(statusLogCountAfterFirst);
        assertThat(fixture.taskRecordRepository.findStatusByTaskNo(task.taskNo())).contains("SUCCESS");
    }
}

