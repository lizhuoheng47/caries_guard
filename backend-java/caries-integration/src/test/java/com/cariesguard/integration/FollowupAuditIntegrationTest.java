package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.followup.domain.model.FupPlanModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.interfaces.command.UpdateFollowupTaskStatusCommand;
import com.cariesguard.followup.interfaces.vo.FollowupTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FollowupAuditIntegrationTest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldKeepStateMachineAndNotifyTraceWhenFollowupTriggered() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "followup-audit"));
        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        fixture.analysisCallbackAppService.handleResultCallback(
                callbackBody, timestamp, fixture.signCallback(callbackBody, timestamp));
        long reportId = fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "followup audit", null)).reportId();

        assertThat(fixture.state.caseStatusCode).isEqualTo("FOLLOWUP_REQUIRED");
        assertThat(fixture.state.followupRequiredFlag).isEqualTo("1");

        CaseStatusLogCreateModel analysisTransition = findTransition("ANALYZING", "REVIEW_PENDING");
        assertThat(analysisTransition.changeReasonCode()).isEqualTo("AI_CALLBACK_SUCCESS");
        CaseStatusLogCreateModel reportTransition = findTransition("REVIEW_PENDING", "REPORT_READY");
        assertThat(reportTransition.changeReasonCode()).isEqualTo("DOCTOR_CONFIRMED");
        CaseStatusLogCreateModel followupTransition = findTransition("REPORT_READY", "FOLLOWUP_REQUIRED");
        assertThat(followupTransition.changeReasonCode()).isEqualTo("FOLLOWUP_TRIGGERED");

        List<FupPlanModel> plans = fixture.fupPlanRepository.all();
        assertThat(plans).hasSize(1);
        FupPlanModel plan = plans.get(0);
        assertThat(plan.triggerSourceCode()).isEqualTo("RISK_HIGH");
        assertThat(plan.triggerRefId()).isEqualTo(reportId);
        assertThat(plan.planStatusCode()).isEqualTo("ACTIVE");

        List<FupTaskModel> tasks = fixture.fupTaskRepository.all();
        assertThat(tasks).hasSize(1);
        FupTaskModel followupTask = tasks.get(0);
        assertThat(followupTask.planId()).isEqualTo(plan.planId());
        assertThat(followupTask.taskStatusCode()).isEqualTo("TODO");

        assertThat(fixture.msgNotifyRepository.all())
                .anySatisfy(notify -> {
                    assertThat(notify.bizModuleCode()).isEqualTo("FOLLOWUP");
                    assertThat(notify.bizId()).isEqualTo(followupTask.taskId());
                    assertThat(notify.notifyTypeCode()).isEqualTo("REMINDER");
                    assertThat(notify.channelCode()).isEqualTo("IN_APP");
                    assertThat(notify.sendStatusCode()).isEqualTo("PENDING");
                    assertThat(notify.orgId()).isEqualTo(fixture.state.orgId);
                });
    }

    @Test
    void shouldAutoClosePlanWhenSingleTaskCompleted() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId,
                fixture.state.patientId,
                fixture.state.orgId,
                9901L,
                "HIGH",
                "1",
                30,
                100001L);

        FupTaskModel currentTask = fixture.fupTaskRepository.all().get(0);
        FollowupTaskVO updated = fixture.followupTaskAppService.updateTaskStatus(
                currentTask.taskId(),
                new UpdateFollowupTaskStatusCommand("DONE", "completed in audit test"));

        assertThat(updated.taskStatusCode()).isEqualTo("DONE");
        assertThat(updated.completedAt()).isNotNull();
        assertThat(fixture.fupPlanRepository.findById(currentTask.planId()))
                .get()
                .matches(plan -> "DONE".equals(plan.planStatusCode()));
    }

    private CaseStatusLogCreateModel findTransition(String from, String to) {
        return fixture.state.caseStatusLogs.stream()
                .filter(item -> from.equals(item.fromStatusCode()) && to.equals(item.toStatusCode()))
                .findFirst()
                .orElseThrow();
    }
}
