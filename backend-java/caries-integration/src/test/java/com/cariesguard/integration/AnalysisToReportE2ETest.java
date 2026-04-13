package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AnalysisToReportE2ETest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldRunGoldenPathFromAnalysisToReportAndExportAudit() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "e2e-golden"));

        assertThat(task.taskStatusCode()).isEqualTo("QUEUEING");
        assertThat(fixture.state.caseStatusCode).isEqualTo("ANALYZING");

        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        String signature = fixture.signCallback(callbackBody, timestamp);
        AnalysisCallbackAckVO ack = fixture.analysisCallbackAppService.handleResultCallback(callbackBody, timestamp, signature);

        assertThat(ack.taskStatusCode()).isEqualTo("SUCCESS");
        assertThat(fixture.taskRecordRepository.findStatusByTaskNo(task.taskNo())).contains("SUCCESS");
        assertThat(fixture.summaryRepository.findByTaskId(task.taskId())).isPresent();
        assertThat(fixture.visualAssetRepository.listByTaskId(task.taskId())).hasSize(1);
        assertThat(fixture.riskRepository.records()).hasSize(1);
        assertThat(fixture.state.caseStatusCode).isEqualTo("REVIEW_PENDING");

        CaseStatusLogCreateModel aiTransition = findTransition(fixture.state.caseStatusLogs, "ANALYZING", "REVIEW_PENDING");
        assertThat(aiTransition.changeReasonCode()).isEqualTo("AI_CALLBACK_SUCCESS");

        ReportGenerateResultVO report = fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "Doctor confirmed", null));

        assertThat(report.reportTypeCode()).isEqualTo("DOCTOR");
        assertThat(report.versionNo()).isEqualTo(1);
        assertThat(report.reportStatusCode()).isEqualTo("FINAL");
        assertThat(fixture.reportRecordRepository.listByCaseId(fixture.state.caseId)).hasSize(1);
        assertThat(fixture.reportRecordRepository.findById(report.reportId())).get().matches(item -> item.attachmentId() != null);
        assertThat(fixture.state.caseStatusCode).isEqualTo("REPORT_READY");

        CaseStatusLogCreateModel reportTransition = findTransition(fixture.state.caseStatusLogs, "REVIEW_PENDING", "REPORT_READY");
        assertThat(reportTransition.changeReasonCode()).isEqualTo("DOCTOR_CONFIRMED");

        ReportExportResultVO export = fixture.reportAppService.exportReport(report.reportId(), new ExportReportCommand(null, null));
        assertThat(export.exported()).isTrue();
        assertThat(fixture.reportExportLogRepository.logs()).hasSize(1);
        ReportExportLogModel log = fixture.reportExportLogRepository.logs().get(0);
        assertThat(log.reportId()).isEqualTo(report.reportId());
        assertThat(log.exportTypeCode()).isEqualTo("PDF");
        assertThat(log.exportChannelCode()).isEqualTo("DOWNLOAD");
        assertThat(log.exportedBy()).isNotNull();
        assertThat(log.exportedAt()).isNotNull();
        assertThat(log.orgId()).isEqualTo(fixture.state.orgId);
    }

    @Test
    void shouldRejectReportGenerationBeforeReviewPending() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        assertThatThrownBy(() -> fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "Not ready", null)))
                .isInstanceOf(BusinessException.class);

        assertThat(fixture.reportRecordRepository.listByCaseId(fixture.state.caseId)).isEmpty();
        assertThat(fixture.state.caseStatusCode).isEqualTo("QC_PENDING");
    }

    private CaseStatusLogCreateModel findTransition(List<CaseStatusLogCreateModel> logs, String from, String to) {
        return logs.stream()
                .filter(item -> from.equals(item.fromStatusCode()) && to.equals(item.toStatusCode()))
                .findFirst()
                .orElseThrow();
    }
}
