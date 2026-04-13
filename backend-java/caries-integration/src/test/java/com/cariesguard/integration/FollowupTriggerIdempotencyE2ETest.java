package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 幂等场景测试：同一触发来源 + 同一触发参考ID，不重复建随访计划。
 * <p>
 * 对应 P6 文档 7.2 幂等场景要求。
 */
class FollowupTriggerIdempotencyE2ETest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldNotCreateDuplicatePlanWhenTriggerCalledTwiceWithSameReportId() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        // 准备：完成分析 -> 生成报告
        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "idempotency-test"));
        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        fixture.analysisCallbackAppService.handleResultCallback(callbackBody, timestamp,
                fixture.signCallback(callbackBody, timestamp));

        ReportGenerateResultVO report = fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "first generate", null));

        // 第一次触发：应创建计划
        assertThat(fixture.fupPlanRepository.all()).hasSize(1);
        assertThat(fixture.fupTaskRepository.all()).hasSize(1);
        String firstPlanNo = fixture.fupPlanRepository.all().get(0).planNo();

        // 直接再次调用 triggerFromReport，同一 reportId（模拟 callback 重放或重复生成）
        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId,
                fixture.state.patientId,
                fixture.state.orgId,
                report.reportId(),
                "HIGH",
                "1",
                30,
                100001L);

        // 断言：仍然只有一个计划，没有重复创建
        assertThat(fixture.fupPlanRepository.all()).hasSize(1);
        assertThat(fixture.fupTaskRepository.all()).hasSize(1);
        assertThat(fixture.fupPlanRepository.all().get(0).planNo()).isEqualTo(firstPlanNo);
    }

    @Test
    void shouldCreateNewPlanForDifferentReportId() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        // 第一次触发：reportId = 9001
        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId, fixture.state.patientId, fixture.state.orgId,
                9001L, "HIGH", "1", 30, 100001L);

        assertThat(fixture.fupPlanRepository.all()).hasSize(1);

        // 重置病例状态为 REPORT_READY（便于下一次触发时能迁移至 FOLLOWUP_REQUIRED）
        fixture.state.caseStatusCode = "REPORT_READY";

        // 第二次触发：不同的 reportId = 9002（不同来源，允许创建新计划）
        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId, fixture.state.patientId, fixture.state.orgId,
                9002L, "HIGH", "1", 30, 100001L);

        // 断言：两个不同来源各有一个计划
        assertThat(fixture.fupPlanRepository.all()).hasSize(2);
    }
}
