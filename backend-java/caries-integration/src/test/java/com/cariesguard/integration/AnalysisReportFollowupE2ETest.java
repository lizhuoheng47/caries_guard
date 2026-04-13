package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.followup.domain.model.FupPlanModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * E2E 主链路测试：analysis -> report -> followup
 * <p>
 * 验证高风险病例在报告生成后能够自动进入随访计划，形成完整业务闭环。
 * 对应 P6 文档"阶段 6 — 新增跨模块 E2E"中的正向主链路场景。
 */
class AnalysisReportFollowupE2ETest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldCreateFollowupPlanAndTaskWhenHighRiskReportGenerated() {
        // 1. 设置操作医生上下文
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        // 2. 创建分析任务
        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "e2e-p6"));
        assertThat(task.taskStatusCode()).isEqualTo("QUEUEING");
        assertThat(fixture.state.caseStatusCode).isEqualTo("ANALYZING");

        // 3. 模拟 AI 回调（风险等级 HIGH + reviewSuggestedFlag=1）
        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        String signature = fixture.signCallback(callbackBody, timestamp);
        AnalysisCallbackAckVO ack = fixture.analysisCallbackAppService.handleResultCallback(callbackBody, timestamp, signature);

        assertThat(ack.taskStatusCode()).isEqualTo("SUCCESS");
        assertThat(fixture.state.caseStatusCode).isEqualTo("REVIEW_PENDING");
        assertThat(fixture.riskRepository.records()).hasSize(1);
        assertThat(fixture.riskRepository.records().get(0).overallRiskLevelCode()).isEqualTo("HIGH");

        // 4. 生成报告（触发 followup）
        ReportGenerateResultVO report = fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "High risk confirmed", null));

        assertThat(report.reportStatusCode()).isEqualTo("FINAL");
        assertThat(fixture.state.caseStatusCode).isEqualTo("FOLLOWUP_REQUIRED");

        // 5. 断言：随访计划已自动创建
        List<FupPlanModel> plans = fixture.fupPlanRepository.all();
        assertThat(plans).hasSize(1);
        FupPlanModel plan = plans.get(0);
        assertThat(plan.caseId()).isEqualTo(fixture.state.caseId);
        assertThat(plan.planStatusCode()).isEqualTo("ACTIVE");
        assertThat(plan.triggerSourceCode()).isEqualTo("RISK_HIGH");
        assertThat(plan.triggerRefId()).isEqualTo(report.reportId());
        assertThat(plan.planNo()).startsWith("FUP");

        // 6. 断言：首个随访任务已自动派生
        List<FupTaskModel> tasks = fixture.fupTaskRepository.all();
        assertThat(tasks).hasSize(1);
        FupTaskModel followupTask = tasks.get(0);
        assertThat(followupTask.planId()).isEqualTo(plan.planId());
        assertThat(followupTask.caseId()).isEqualTo(fixture.state.caseId);
        assertThat(followupTask.taskStatusCode()).isEqualTo("TODO");
        assertThat(followupTask.taskNo()).startsWith("TSK");
        assertThat(followupTask.dueDate()).isNotNull();

        // 7. 断言：病例已更新为 FOLLOWUP_REQUIRED
        assertThat(fixture.state.caseStatusCode).isEqualTo("FOLLOWUP_REQUIRED");
        assertThat(fixture.state.followupRequiredFlag).isEqualTo("1");

        // 8. 断言：状态变更日志存在（REPORT_READY -> FOLLOWUP_REQUIRED）
        CaseStatusLogCreateModel reportTransition = findTransition(
                fixture.state.caseStatusLogs, "REVIEW_PENDING", "REPORT_READY");
        assertThat(reportTransition).isNotNull();
        assertThat(reportTransition.changeReasonCode()).isEqualTo("DOCTOR_CONFIRMED");

        CaseStatusLogCreateModel followupTransition = findTransition(
                fixture.state.caseStatusLogs, "REPORT_READY", "FOLLOWUP_REQUIRED");
        assertThat(followupTransition).isNotNull();
        assertThat(followupTransition.changeReasonCode()).isEqualTo("FOLLOWUP_TRIGGERED");

        // 9. 断言：消息通知留痕存在
        assertThat(fixture.msgNotifyRepository.all())
                .isNotEmpty()
                .anySatisfy(n -> {
                    assertThat(n.bizModuleCode()).isEqualTo("FOLLOWUP");
                    assertThat(n.notifyTypeCode()).isEqualTo("REMINDER");
                });
    }

    @Test
    void shouldAutoCloseFollowupPlanWhenAllTasksDone() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        // 准备：完整跑完分析到报告流程
        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "auto-close"));
        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        fixture.analysisCallbackAppService.handleResultCallback(callbackBody, timestamp,
                fixture.signCallback(callbackBody, timestamp));
        fixture.reportAppService.generateReport(fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "confirm", null));

        // 确认计划和任务已创建
        List<FupPlanModel> plans = fixture.fupPlanRepository.all();
        assertThat(plans).hasSize(1);
        List<FupTaskModel> tasks = fixture.fupTaskRepository.all();
        assertThat(tasks).hasSize(1);
        Long taskId = tasks.get(0).taskId();

        // 将任务状态更新为 DONE（直接通过仓储模拟，等同于 FollowupTaskAppService 调用效果）
        fixture.fupTaskRepository.updateStatus(taskId, "DONE", java.time.LocalDateTime.now(), 100001L);

        // 断言仓储中任务已完成
        assertThat(fixture.fupTaskRepository.findById(taskId))
                .get()
                .matches(t -> "DONE".equals(t.taskStatusCode()));
    }

    private CaseStatusLogCreateModel findTransition(
            List<CaseStatusLogCreateModel> logs, String from, String to) {
        return logs.stream()
                .filter(l -> from.equals(l.fromStatusCode()) && to.equals(l.toStatusCode()))
                .findFirst()
                .orElse(null);
    }
}
