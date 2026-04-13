package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.interfaces.command.UpdateFollowupTaskStatusCommand;
import com.cariesguard.followup.interfaces.vo.FollowupTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FollowupOverdueIntegrationTest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldMarkTaskOverdueAndCreateAlertNotify() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId,
                fixture.state.patientId,
                fixture.state.orgId,
                9801L,
                "HIGH",
                "1",
                30,
                100001L);
        FupTaskModel task = fixture.fupTaskRepository.all().get(0);

        FollowupTaskVO updated = fixture.followupTaskAppService.updateTaskStatus(
                task.taskId(),
                new UpdateFollowupTaskStatusCommand("OVERDUE", "due date exceeded"));

        assertThat(updated.taskStatusCode()).isEqualTo("OVERDUE");
        assertThat(updated.completedAt()).isNull();
        assertThat(fixture.fupTaskRepository.findById(task.taskId()))
                .get()
                .matches(item -> "OVERDUE".equals(item.taskStatusCode()));

        assertThat(fixture.msgNotifyRepository.all())
                .anySatisfy(notify -> {
                    assertThat(notify.bizModuleCode()).isEqualTo("FOLLOWUP");
                    assertThat(notify.bizId()).isEqualTo(task.taskId());
                    assertThat(notify.notifyTypeCode()).isEqualTo("ALERT");
                });
    }

    @Test
    void shouldRejectOverdueTransitionForDoneTask() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId,
                fixture.state.patientId,
                fixture.state.orgId,
                9802L,
                "HIGH",
                "1",
                30,
                100001L);
        FupTaskModel task = fixture.fupTaskRepository.all().get(0);

        fixture.followupTaskAppService.updateTaskStatus(
                task.taskId(),
                new UpdateFollowupTaskStatusCommand("DONE", "done first"));

        assertThatThrownBy(() -> fixture.followupTaskAppService.updateTaskStatus(
                task.taskId(),
                new UpdateFollowupTaskStatusCommand("OVERDUE", "should fail")))
                .isInstanceOf(BusinessException.class);
    }
}
