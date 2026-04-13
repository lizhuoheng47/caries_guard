package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 非触发场景测试：低风险、无复查建议时不生成随访计划。
 * <p>
 * 对应 P6 文档 7.3 非触发场景要求。
 */
class FollowupNonTriggerIntegrationTest {

    @AfterEach
    void tearDown() {
        // 每个测试使用独立 fixture，无需清理
    }

    @Test
    void shouldNotTriggerFollowupWhenRiskIsLowAndNoReviewSuggested() {
        AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        // 直接调用 triggerFromReport，低风险、无复查建议
        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId,
                fixture.state.patientId,
                fixture.state.orgId,
                8801L,
                "LOW",      // 风险等级：低
                "0",        // 不建议复查
                null,
                100001L);

        // 断言：不生成计划，不生成任务，不修改病例状态
        assertThat(fixture.fupPlanRepository.all()).isEmpty();
        assertThat(fixture.fupTaskRepository.all()).isEmpty();
        // 病例状态应保持原始值 QC_PENDING（未被 followup 触发器修改）
        assertThat(fixture.state.caseStatusCode).isEqualTo("QC_PENDING");
    }

    @Test
    void shouldNotTriggerFollowupWhenRiskIsMediumAndNoReviewSuggested() {
        AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId, fixture.state.patientId, fixture.state.orgId,
                8802L, "MEDIUM", "0", null, 100001L);

        assertThat(fixture.fupPlanRepository.all()).isEmpty();
        assertThat(fixture.fupTaskRepository.all()).isEmpty();
    }

    @Test
    void shouldTriggerFollowupWhenRiskIsLowButReviewSuggestedFlagIsSet() {
        // review_suggested=1 即使风险等级低也应触发随访
        AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        fixture.followupTriggerService.triggerFromReport(
                fixture.state.caseId, fixture.state.patientId, fixture.state.orgId,
                8803L, "LOW", "1",  // 低风险但建议复查
                null, 100001L);

        assertThat(fixture.fupPlanRepository.all()).hasSize(1);
        assertThat(fixture.fupPlanRepository.all().get(0).triggerSourceCode()).isEqualTo("REPORT_REVIEW");
        assertThat(fixture.fupTaskRepository.all()).hasSize(1);
    }

    @Test
    void shouldNotModifyCaseStatusIfReportGeneratedWithLowRisk() {
        // 完整跑报告流程但用低风险回调，确认不创建随访计划，病例状态也不进入 FOLLOWUP_REQUIRED
        AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "low-risk-test"));

        // 构造低风险回调（overallRiskLevelCode=LOW, reviewSuggestedFlag=0）
        String lowRiskCallback = """
                {
                  "taskNo":"%s",
                  "taskStatusCode":"SUCCESS",
                  "modelVersion":"caries-v1",
                  "summary":{"overallHighestSeverity":"C1","uncertaintyScore":0.05,"reviewSuggestedFlag":"0"},
                  "rawResultJson":{"overall_highest_severity":"C1","uncertainty_score":0.05,"review_suggested_flag":"0"},
                  "visualAssets":[],
                  "riskAssessment":{"overallRiskLevelCode":"LOW","assessmentReportJson":{"score":20},"recommendedCycleDays":90}
                }
                """.formatted(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        fixture.analysisCallbackAppService.handleResultCallback(
                lowRiskCallback, timestamp, fixture.signCallback(lowRiskCallback, timestamp));

        fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "low risk confirmed", null));

        // 断言：未创建随访计划
        assertThat(fixture.fupPlanRepository.all()).isEmpty();
        assertThat(fixture.fupTaskRepository.all()).isEmpty();
        // 病例状态应停在 REPORT_READY，不进入 FOLLOWUP_REQUIRED
        assertThat(fixture.state.caseStatusCode).isEqualTo("REPORT_READY");
        assertThat(fixture.state.followupRequiredFlag).isEqualTo("0");
    }
}
